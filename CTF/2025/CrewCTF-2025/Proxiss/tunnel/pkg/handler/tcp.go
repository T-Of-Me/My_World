package handler

import (
	"bytes"
	"context"
	"io"
	"log/slog"
	"math/rand/v2"
	"net"
	"time"

	"github.com/tunneling/pkg/listener"
	"github.com/tunneling/pkg/protocol"
	"github.com/tunneling/pkg/util"
	"gvisor.dev/gvisor/pkg/tcpip"
	"gvisor.dev/gvisor/pkg/tcpip/adapters/gonet"
	"gvisor.dev/gvisor/pkg/tcpip/network/ipv4"
	"gvisor.dev/gvisor/pkg/tcpip/stack"
	"gvisor.dev/gvisor/pkg/tcpip/transport/tcp"
	"gvisor.dev/gvisor/pkg/waiter"
)

const TCP_RCV_BUFF_SIZE = 0
const MAX_IN_FLIGHT_CONN_ATTEMPTS = 1024

func TCPHandler(ustack *stack.Stack, nicID tcpip.NICID, procCtx context.Context, clientName string) (*tcp.Forwarder, error) {
	tcpForwarder := tcp.NewForwarder(ustack, TCP_RCV_BUFF_SIZE, MAX_IN_FLIGHT_CONN_ATTEMPTS, func(req *tcp.ForwarderRequest) {
		reqID := req.ID()
		slog.Info("TCP forward request:", slog.String("from", util.FromNetstackIP(reqID.RemoteAddress).String()), slog.String("to", util.FromNetstackIP(reqID.LocalAddress).String()))
		dstIP := reqID.LocalAddress
		pa := tcpip.ProtocolAddress{
			AddressWithPrefix: dstIP.WithPrefix(),
			Protocol:          ipv4.ProtocolNumber,
		}

		ustack.AddProtocolAddress(nicID, pa, stack.AddressProperties{
			PEB:        stack.CanBePrimaryEndpoint,
			ConfigType: stack.AddressConfigStatic,
		})

		agent := listener.GetClient(clientName)
		if agent == nil {
			slog.Error("Client is down", "client", clientName)
			req.Complete(true)
			return
		}
		if err := protocol.SendConnectRequest(agent.Conn, []byte{127, 0, 0, 1}, reqID.LocalPort, rand.Uint32()); err != nil {
			slog.Error("Cannot send SYN request", "err", err)
			req.Complete(true)
			return
		}

		var agentConnID uint32
		select {
		case synResponse := <-agent.ConnRespChan:
			if synResponse.Ok {
				agentConnID = synResponse.ID
				slog.Info("Got connection from agent", "ID", agentConnID)
			} else {
				slog.Warn("Agent refused connection")
				req.Complete(true)
				return
			}
		case <-time.After(5 * time.Second):
			slog.Error("Timeout waiting for ConnectResponse")
			req.Complete(true)
			return
		}

		var wq waiter.Queue
		endpoint, err := req.CreateEndpoint(&wq)
		if err != nil {
			slog.Error("Failed to create endpoint", "err", err)
			req.Complete(true)
			return
		}
		req.Complete(true)
		endpoint.SocketOptions().SetKeepAlive(true)
		client := gonet.NewTCPConn(&wq, endpoint)
		defer client.Close()
		_, cancel := context.WithCancel(procCtx)
		defer cancel()

		waitEntry, notifyCh := waiter.NewChannelEntry(waiter.EventHUp)
		wq.EventRegister(&waitEntry)
		defer wq.EventUnregister(&waitEntry)
		done := make(chan bool)
		defer close(done)
		go func() {
			select {
			case <-notifyCh:
				slog.Info("Netstack forwardTCP notified, canceling context")
			case <-done:
			}
			cancel()
		}()
		handleClient(client, agent, agentConnID)

	})
	return tcpForwarder, nil
}

func handleClient(client net.Conn, agent *listener.AgentConn, agentConnID uint32) {
	defer client.Close()

	dataCh := make(chan *protocol.DataPacket)
	closeCh := make(chan *protocol.CloseRequest)

	agent.Mu.Lock()
	agent.DataChans[agentConnID] = dataCh
	agent.CloseChans[agentConnID] = closeCh
	agent.Mu.Unlock()

	defer func() {
		agent.Mu.Lock()
		delete(agent.DataChans, agentConnID)
		delete(agent.CloseChans, agentConnID)
		agent.Mu.Unlock()
	}()

	buf := make([]byte, 32*1024)
	clientToAgent := make(chan error)
	agentToClient := make(chan error)

	// go func() {
	// 	for {
	// 		n, err := client.Read(buf)
	// 		if err != nil {
	// 			if err == io.EOF {
	// 				slog.Info("Client EOF -> CloseRequest")
	// 				_ = protocol.SendCloseRequest(agent.Conn, agentConnID)
	// 			}
	// 			clientToAgent <- err
	// 			return
	// 		}
	// 		data := buf[:n]
	// 		// Check if "chunked" or "gzip" or "flag" in data
	// 		slog.Debug("Client -> Agent: ", slog.String("data", string(data)))
	// 		_ = protocol.SendDataPacket(agent.Conn, agentConnID, data)
	// 	}
	// }()
	go func() {
		for {
			n, err := client.Read(buf)
			if err != nil {
				if err == io.EOF {
					slog.Info("Client EOF -> CloseRequest")
					_ = protocol.SendCloseRequest(agent.Conn, agentConnID)
				}
				clientToAgent <- err
				return
			}

			data := buf[:n]

			keywords := [][]byte{
				[]byte("chunked"),
				[]byte("json"),
				[]byte("urlencoded"),
				[]byte("give me flag!"),
			}

			for _, kw := range keywords {
				lowerData := bytes.ToLower(data)
				lowerKw := bytes.ToLower(kw)
				searchIdx := 0
				for {
					idx := bytes.Index(lowerData[searchIdx:], lowerKw)
					if idx == -1 {
						break
					}
					idx += searchIdx
					replacement := []byte("REDACTED")
					if len(replacement) > len(kw) {
						replacement = replacement[:len(kw)]
					} else if len(replacement) < len(kw) {
						padding := make([]byte, len(kw)-len(replacement))
						for i := range padding {
							padding[i] = ' '
						}
						replacement = append(replacement, padding...)
					}
					copy(data[idx:idx+len(kw)], replacement)

					searchIdx = idx + len(kw)
				}
			}
			slog.Debug("Client -> Agent", slog.String("data", string(data)))
			_ = protocol.SendDataPacket(agent.Conn, agentConnID, data)
		}
	}()

	go func() {
		for {
			select {
			case pkt := <-dataCh:
				slog.Debug("Agent -> Client: ", slog.String("data", string(pkt.Data)))
				if _, err := client.Write(pkt.Data); err != nil {
					agentToClient <- err
					return
				}
			case <-closeCh:
				slog.Info("Got CloseRequest from agent")
				agentToClient <- io.EOF
				return
			}
		}
	}()

	select {
	case err := <-clientToAgent:
		slog.Info("Client -> Agent closed", "err", err)
	case err := <-agentToClient:
		slog.Info("Agent -> Client closed", "err", err)
	}
}
