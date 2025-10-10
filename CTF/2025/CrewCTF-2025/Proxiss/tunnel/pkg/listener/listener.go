package listener

import (
	"bufio"
	"context"
	"fmt"
	"log/slog"
	"net"
	"strings"
	"sync"
	"time"

	"github.com/tunneling/pkg/config"
	"github.com/tunneling/pkg/protocol"
)

const MAX_CONCURRENT = 100

type AgentConn struct {
	Conn net.Conn

	Mu         sync.Mutex
	DataChans  map[uint32]chan *protocol.DataPacket
	CloseChans map[uint32]chan *protocol.CloseRequest

	ConnRespChan chan *protocol.ConnectResponse
}

var (
	clients   = make(map[string]*AgentConn)
	clientsMu sync.Mutex
)

func (ac *AgentConn) readLoop(name string) {
	defer func() {
		DeleteClient(name)
		slog.Info("Connection is closed and removed", "client", name)
	}()
	dec := protocol.NewDecoder(ac.Conn)
	for {
		if err := dec.Decode(); err != nil {
			slog.Error("AgentConn decode failed", "err", err)
			return
		}

		switch pkt := dec.Payload.(type) {

		case *protocol.ConnectResponse:
			ac.ConnRespChan <- pkt

		case *protocol.DataPacket:
			ac.Mu.Lock()
			ch, ok := ac.DataChans[pkt.ID]
			ac.Mu.Unlock()
			if ok {
				ch <- pkt
			} else {
				slog.Warn("No handler for DataPacket", "ID", pkt.ID)
			}

		case *protocol.CloseRequest:
			ac.Mu.Lock()
			ch, ok := ac.CloseChans[pkt.ID]
			ac.Mu.Unlock()
			if ok {
				ch <- pkt
			} else {
				slog.Warn("No handler for CloseRequest", "ID", pkt.ID)
			}

		default:
			slog.Warn("Unknown packet type", "type", fmt.Sprintf("%T", pkt))
		}
	}
}

func SpawnListener(ctx context.Context, listenIP string) error {
	if listenIP == "" {
		listenIP = "127.0.0.1:19001"
	}
	ln, err := net.Listen("tcp", listenIP)
	if err != nil {
		return err
	}

	go func() {
		<-ctx.Done()
		_ = ln.Close()
	}()

	go func() {
		for {
			conn, err := ln.Accept()
			if err != nil {
				select {
				case <-ctx.Done():
					return
				default:
					slog.Error("Accept failed", "err", err)
					continue
				}
			}

			go func(c net.Conn) {
				name, ok := registerConnection(c, 10*time.Second)
				if !ok {
					c.Close()
					return
				}
				handleClient(name)
			}(conn)
		}
	}()

	return nil
}

func handleClient(name string) {
	slog.Info("Connection is now alive with", "client", name)
	ac := GetClient(name)
	if ac == nil {
		slog.Error("Cannot get the client", "name", name)
		return
	}
	go ac.readLoop(name)
}

func registerConnection(conn net.Conn, timeout time.Duration) (string, bool) {
	conn.SetReadDeadline(time.Now().Add(timeout))

	reader := bufio.NewReader(conn)
	nameRaw, err := reader.ReadString('\n')
	if err != nil {
		if netErr, ok := err.(net.Error); ok && netErr.Timeout() {
			slog.Error("Client read name timeout after", slog.Duration("interval", timeout))
		} else {
			slog.Error("Failed to read name", "err", err)
		}
		return "", false
	}

	conn.SetReadDeadline(time.Time{})

	name := strings.TrimSpace(nameRaw)
	if name == "" {
		return "", false
	}
	if name != config.AgentName {
		return "", false
	}
	if c := GetClient(name); c != nil {
		c.Conn.Close()
	}
	clientsMu.Lock()
	clients[name] = &AgentConn{
		Conn:         conn,
		DataChans:    make(map[uint32]chan *protocol.DataPacket),
		CloseChans:   make(map[uint32]chan *protocol.CloseRequest),
		ConnRespChan: make(chan *protocol.ConnectResponse, 1),
	}
	clientsMu.Unlock()
	slog.Info("Client connected", "name", name)
	return name, true
}

func GetClientNames() []string {
	names := make([]string, 0, len(clients))
	for name := range clients {
		names = append(names, name)
	}
	return names
}

func GetClient(name string) *AgentConn {
	clientsMu.Lock()
	c, ok := clients[name]
	clientsMu.Unlock()
	if !ok {
		return nil
	}
	return c
}

func Ping(conn net.Conn) bool {
	if err := protocol.SendPingRequest(conn); err != nil {
		slog.Error("Failed to send PingRequest", "err", err)
		return false
	}
	return true
}

func DeleteClient(name string) {
	clientsMu.Lock()
	c, ok := clients[name]
	clientsMu.Unlock()
	if !ok {
		return
	}
	c.Conn.Close()
	clientsMu.Lock()
	delete(clients, name)
	clientsMu.Unlock()
}
