package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/tunneling/pkg/config"
	"github.com/tunneling/pkg/handler"
	"github.com/tunneling/pkg/listener"
	"github.com/tunneling/pkg/netstack"
	"gvisor.dev/gvisor/pkg/tcpip/transport/tcp"
)

func main() {
	procCtx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	err := listener.SpawnListener(procCtx, "0.0.0.0:19001")
	if err != nil {
		log.Panicf("Error spawning listener: %s", err)
	}

	s, err := netstack.New(config.TUNName, config.AgentName)
	if err != nil {
		log.Panicf("Error: %v", err)
	}
	ustack, nicID, dev, linkEP := s.Ustack, s.NicID, s.Dev, s.LinkEP

	tcpFwd, err := handler.TCPHandler(ustack, nicID, procCtx, s.ClientName)
	if err != nil {
		log.Panicf("Error TCP Forwarder: %v", err)
	}
	ustack.SetTransportProtocolHandler(tcp.ProtocolNumber, tcpFwd.HandlePacket)

	go netstack.ForwardTunnelToEndpoint(procCtx, dev, linkEP)
	go netstack.ForwardEndpointToTunnel(procCtx, linkEP, dev)

	statsC := make(chan os.Signal, 1)
	signal.Notify(statsC, syscall.SIGUSR1)
	go func() {
		for {
			select {
			case <-procCtx.Done():
				return
			case <-statsC:
				stats := ustack.Stats()
				log.Printf("Got USR1, printing TCP stats:\n\tip-malformed-packets-received: %s\n\ttotal-packets-received-bytes: %s\n\ttotal-packets-received-count: %s\n",
					stats.IP.MalformedPacketsReceived,
					stats.NICs.Rx.Bytes,
					stats.NICs.Rx.Packets,
				)
			}

		}
	}()

	<-procCtx.Done()
	log.Print("Got exit singal. Shutting down.")

	ustack.RemoveNIC(nicID)
	linkEP.Close()

}
