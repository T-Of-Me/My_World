package netstack

import (
	"bytes"
	"context"
	"fmt"
	"log/slog"
	"net"
	"sync"

	"github.com/tunneling/pkg/config"
	"golang.zx2c4.com/wireguard/device"
	"golang.zx2c4.com/wireguard/tun"
	"gvisor.dev/gvisor/pkg/buffer"
	"gvisor.dev/gvisor/pkg/tcpip"
	"gvisor.dev/gvisor/pkg/tcpip/header"
	"gvisor.dev/gvisor/pkg/tcpip/link/channel"
	"gvisor.dev/gvisor/pkg/tcpip/network/ipv4"
	"gvisor.dev/gvisor/pkg/tcpip/stack"
	"gvisor.dev/gvisor/pkg/tcpip/transport/tcp"
)

var (
	netstacks   = make(map[string]*NetStack)
	netstacksMu sync.Mutex
)

type NetStack struct {
	ClientName string
	Ustack     *stack.Stack
	NicID      tcpip.NICID
	Dev        tun.Device
	LinkEP     *channel.Endpoint
}

func New(name string, clientName string) (*NetStack, error) {

	netstacksMu.Lock()
	if ns, exists := netstacks[name]; exists {
		netstacksMu.Unlock()
		return ns, nil
	}
	netstacksMu.Unlock()

	ustack := stack.New(stack.Options{
		NetworkProtocols:   []stack.NetworkProtocolFactory{ipv4.NewProtocol},
		TransportProtocols: []stack.TransportProtocolFactory{tcp.NewProtocol},
	})

	slog.Info("Setting up TUN device with parameters", slog.Int("mtu", config.MTU))

	dev, err := tun.CreateTUN(name, config.MTU)
	if err != nil {
		return nil, fmt.Errorf("failed to create tun device: %s", err)
	}
	devName, err := dev.Name()
	if err != nil {
		return nil, fmt.Errorf("failed to get device name: %s", err)
	}
	slog.Info("Created tun device", "name", devName)

	nicID := ustack.NextNICID()
	linkEP := channel.New(512, uint32(config.MTU), "")

	if err := ustack.CreateNIC(nicID, linkEP); err != nil {
		return nil, fmt.Errorf("can't create nic: %v", err)
	}

	ustack.SetPromiscuousMode(nicID, true)

	tcpRoute := []tcpip.Route{}

	ip4, ip4net, err := net.ParseCIDR(config.LocalIPv4CIDR)
	if err != nil {
		return nil, fmt.Errorf("failed to parse IPv4 subnet: %s", err)
	}
	ipv4Subnet, err := tcpip.NewSubnet(
		tcpip.AddrFromSlice(ip4.To4()),
		tcpip.MaskFromBytes(net.IP(ip4net.Mask).To4()),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to create IPv4 subnet: %s", err)
	}
	tcpRoute = append(tcpRoute, tcpip.Route{
		Destination: ipv4Subnet,
		NIC:         nicID,
	})
	ustack.SetRouteTable(tcpRoute)

	nstack := &NetStack{
		ClientName: clientName,
		Ustack:     ustack,
		NicID:      nicID,
		Dev:        dev,
		LinkEP:     linkEP,
	}

	netstacksMu.Lock()
	if ns, exists := netstacks[name]; exists {
		netstacksMu.Unlock()
		return ns, nil
	}
	netstacks[name] = nstack
	netstacksMu.Unlock()

	return nstack, nil
}

func ForwardEndpointToTunnel(ctx context.Context, endpoint *channel.Endpoint, tun tun.Device) {
	for {
		packet := endpoint.ReadContext(ctx)
		if packet == nil {
			continue
		}
		buf := packet.ToBuffer()
		bytes := (&buf).Flatten()
		// log.Printf("DEBUG: Forward to Tunnel buf: %x\n", bytes)
		const writeOffset = device.MessageTransportHeaderSize
		moreBytes := make([]byte, writeOffset, len(bytes)+writeOffset)
		moreBytes = append(moreBytes[:writeOffset], bytes...)

		if _, err := tun.Write([][]byte{moreBytes}, writeOffset); err != nil {
			slog.Error("failed to inject inbound", "error", err)
			return
		}
	}
}

func ForwardTunnelToEndpoint(ctx context.Context, tun tun.Device, dstEndpoint *channel.Endpoint) error {
	buffers := make([][]byte, tun.BatchSize())
	for i := range buffers {
		buffers[i] = make([]byte, device.MaxMessageSize)
	}
	const readOffset = device.MessageTransportHeaderSize
	sizes := make([]int, len(buffers))
	for {
		for i := range buffers {
			buffers[i] = buffers[i][:cap(buffers[i])]
		}
		n, err := tun.Read(buffers, sizes, readOffset)
		if err != nil {
			slog.Error("failed to read from tun", "error", err)
			return err
		}
		for i := range sizes[:n] {
			buffers[i] = buffers[i][readOffset : readOffset+sizes[i]]
			// ready to send data to channel
			packetBuf := stack.NewPacketBuffer(stack.PacketBufferOptions{
				Payload: buffer.MakeWithData(bytes.Clone(buffers[i])),
			})
			dstEndpoint.InjectInbound(header.IPv4ProtocolNumber, packetBuf)
			packetBuf.DecRef()
		}
	}
}
