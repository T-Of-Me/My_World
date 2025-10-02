package protocol

import (
	"fmt"
	"io"

	"github.com/shamaton/msgpack/v2"
)

type Encoder struct {
	writer io.Writer
}

func NewEncoder(writer io.Writer) *Encoder {
	return &Encoder{writer: writer}
}

func typeForPayload(payload interface{}) (uint8, error) {
	switch payload.(type) {
	case ConnectRequest:
		return MessageConnectRequest, nil
	case ConnectResponse:
		return MessageConnectResponse, nil
	case CloseRequest:
		return MessageCloseRequest, nil
	case DataPacket:
		return MessageDataPacket, nil
	case PingRequest:
		return MessagePingRequest, nil
	default:
		return 0, fmt.Errorf("unknown payload type: %T", payload)
	}
}

func (e *Encoder) Encode(payload interface{}) error {
	payloadType, err := typeForPayload(payload)
	if err != nil {
		return err
	}
	// Write type byte
	if err := msgpack.MarshalWrite(e.writer, payloadType); err != nil {
		return err
	}
	// Write payload
	if err := msgpack.MarshalWrite(e.writer, payload); err != nil {
		return err
	}
	return nil
}
