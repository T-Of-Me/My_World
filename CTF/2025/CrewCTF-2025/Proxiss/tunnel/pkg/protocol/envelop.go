package protocol

type ConnectRequest struct {
	IP   []byte
	Port uint16
	ID   uint32
}

type ConnectResponse struct {
	Ok bool
	ID uint32
}

type CloseRequest struct {
	ID uint32
}

type DataPacket struct {
	ID   uint32
	Data []byte
}

type PingRequest struct{}

const (
	MessageConnectRequest  = uint8(1)
	MessageConnectResponse = uint8(2)
	MessageCloseRequest    = uint8(3)
	MessageDataPacket      = uint8(4)
	MessagePingRequest     = uint8(5)
)
