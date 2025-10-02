package protocol

import (
	"fmt"
	"net"
)

func ReadConnectRequest(conn net.Conn) (*ConnectRequest, error) {
	dec := NewDecoder(conn)
	if err := dec.Decode(); err != nil {
		return nil, fmt.Errorf("decode failed: %w", err)
	}
	req, ok := dec.Payload.(*ConnectRequest)
	if !ok {
		return nil, fmt.Errorf("unexpected packet type: %T", dec.Payload)
	}
	return req, nil
}

func ReadConnectResponse(conn net.Conn) (*ConnectResponse, error) {
	dec := NewDecoder(conn)
	if err := dec.Decode(); err != nil {
		return nil, fmt.Errorf("decode failed: %w", err)
	}
	resp, ok := dec.Payload.(*ConnectResponse)
	if !ok {
		return nil, fmt.Errorf("unexpected packet type: %T", dec.Payload)
	}
	return resp, nil
}

func ReadDataPacket(conn net.Conn) (*DataPacket, error) {
	dec := NewDecoder(conn)
	if err := dec.Decode(); err != nil {
		return nil, fmt.Errorf("decode failed: %w", err)
	}
	packet, ok := dec.Payload.(*DataPacket)
	if !ok {
		return nil, fmt.Errorf("unexpected packet type: %T", dec.Payload)
	}
	return packet, nil
}

func ReadPingRequest(conn net.Conn) (*PingRequest, error) {
	dec := NewDecoder(conn)
	if err := dec.Decode(); err != nil {
		return nil, err
	}
	req, ok := dec.Payload.(*PingRequest)
	if !ok {
		return nil, fmt.Errorf("unexpected packet type: %T", dec.Payload)
	}
	return req, nil
}
