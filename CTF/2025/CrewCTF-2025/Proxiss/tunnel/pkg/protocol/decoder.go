package protocol

import (
	"fmt"
	"io"

	"github.com/shamaton/msgpack/v2"
)

type Decoder struct {
	reader  io.Reader
	Payload interface{}
}

func NewDecoder(reader io.Reader) *Decoder {
	return &Decoder{reader: reader}
}

func typeToStruct(payloadType uint8) (interface{}, error) {
	switch payloadType {
	case MessageConnectRequest:
		return &ConnectRequest{}, nil
	case MessageConnectResponse:
		return &ConnectResponse{}, nil
	case MessageCloseRequest:
		return &CloseRequest{}, nil
	case MessageDataPacket:
		return &DataPacket{}, nil
	case MessagePingRequest:
		return &PingRequest{}, nil
	default:
		return nil, fmt.Errorf("unknown payload type: %d", payloadType)
	}
}

func (d *Decoder) Decode() error {
	var payloadType uint8
	if err := msgpack.UnmarshalRead(d.reader, &payloadType); err != nil {
		return err
	}

	obj, err := typeToStruct(payloadType)
	if err != nil {
		return err
	}

	if err := msgpack.UnmarshalRead(d.reader, obj); err != nil {
		return err
	}

	d.Payload = obj
	return nil
}
