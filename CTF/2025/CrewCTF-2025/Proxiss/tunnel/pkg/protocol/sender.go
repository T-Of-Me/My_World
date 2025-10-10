package protocol

import (
	"fmt"
	"net"
)

func SendConnectRequest(conn net.Conn, ip []byte, port uint16, id uint32) error {
	enc := NewEncoder(conn)
	req := ConnectRequest{
		IP:   ip,
		Port: port,
		ID:   id,
	}
	if err := enc.Encode(req); err != nil {
		return fmt.Errorf("send connect request failed: %w", err)
	}
	return nil
}

func SendConnectResponse(conn net.Conn, ok bool, id uint32) error {
	enc := NewEncoder(conn)
	resp := ConnectResponse{
		Ok: ok,
		ID: id,
	}
	if err := enc.Encode(resp); err != nil {
		return fmt.Errorf("send connect response failed: %w", err)
	}
	return nil
}

func SendDataPacket(conn net.Conn, id uint32, data []byte) error {
	enc := NewEncoder(conn)
	packet := DataPacket{
		ID:   id,
		Data: data,
	}
	if err := enc.Encode(packet); err != nil {
		return fmt.Errorf("send data packet failed: %w", err)
	}
	return nil
}

func SendCloseRequest(conn net.Conn, id uint32) error {
	enc := NewEncoder(conn)
	return enc.Encode(CloseRequest{ID: id})
}

func SendPingRequest(conn net.Conn) error {
	enc := NewEncoder(conn)
	return enc.Encode(PingRequest{})
}
