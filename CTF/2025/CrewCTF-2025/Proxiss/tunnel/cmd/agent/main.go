package main

import (
	"fmt"
	"log"
	"log/slog"
	"net"
	"os"

	"github.com/tunneling/pkg/config"
	"github.com/tunneling/pkg/protocol"
	"github.com/tunneling/pkg/util"
)

var connections = make(map[uint32]net.Conn)

func main() {
	serverAddr := os.Getenv("SERVER_ADDR")
	if serverAddr == "" {
		fmt.Println("SERVER_ADDR environment variable not set")
		os.Exit(1)
	}
	name := config.AgentName

	conn, err := net.Dial("tcp", serverAddr)
	if err != nil {
		log.Fatalf("Failed to connect: %v", err)
	}
	defer conn.Close()
	log.Printf("Connected to %s", serverAddr)

	// Send name + newline
	fmt.Fprintf(conn, "%s\n", name)
	log.Printf("Sent name: %s", name)
	handleConn(conn)
}

func handleConn(conn net.Conn) {
	defer conn.Close()

	dec := protocol.NewDecoder(conn)

	for {
		if err := dec.Decode(); err != nil {
			log.Fatalf("Decode failed: %v", err)
			continue
		}

		// Copy the payload to avoid reuse issues in concurrent goroutines
		msg := dec.Payload

		go func(payload interface{}) {
			switch m := payload.(type) {
			case *protocol.ConnectRequest:
				addr, err := util.GetAddrPort(m.IP, m.Port)
				if err != nil {
					slog.Error("Cannot get AddrPort", "err", err)
					return
				}
				slog.Info("Receive", "ConnectRequest", addr)

				outConn, err := net.Dial("tcp", addr.String())
				if err != nil {
					slog.Error("Failed to connect to", "addr", addr, "err", err)
					_ = protocol.SendConnectResponse(conn, false, 0)
					return
				}

				connID := util.GenerateConnID(int(m.Port))
				connections[connID] = outConn
				if err := protocol.SendConnectResponse(conn, true, connID); err != nil {
					slog.Error("Failed to send ConnectResponse", "err", err)
					outConn.Close()
					delete(connections, connID)
					return
				}
				slog.Info("Connected", "addr", addr, "ID", connID)

				go func(id uint32, serverConn net.Conn) {
					buf := make([]byte, 32*1024)
					for {
						n, err := serverConn.Read(buf)
						if err != nil {
							slog.Info("Connection closed", "ID", id, "err", err)
							serverConn.Close()
							delete(connections, id)
							return
						}

						if err := protocol.SendDataPacket(conn, id, buf[:n]); err != nil {
							slog.Info("Failed to send DataPacket back", "err", err)
							serverConn.Close()
							delete(connections, id)
							return
						}
					}
				}(connID, outConn)

			case *protocol.DataPacket:
				outConn, ok := connections[m.ID]
				if !ok {
					slog.Error("No connection for", "ID", m.ID)
					return
				}
				if _, err := outConn.Write(m.Data); err != nil {
					slog.Error("Write to connection failed", "ID", m.ID, "err", err)
					outConn.Close()
					delete(connections, m.ID)
				}

			case *protocol.CloseRequest:
				outConn, ok := connections[m.ID]
				if !ok {
					slog.Error("No connection to close for", "ID", m.ID)
					return
				}
				slog.Info("Closing connection by request", "ID", m.ID)
				outConn.Close()
				delete(connections, m.ID)

			case *protocol.PingRequest:
				slog.Info("Got ping")

			default:
				slog.Error("Unknown packet", "type", fmt.Sprintf("%T", m))
			}
		}(msg)
	}
}
