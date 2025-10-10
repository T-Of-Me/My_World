package util

import (
	"crypto/sha256"
	"encoding/binary"
	"fmt"
	"net/netip"
	"time"

	"gvisor.dev/gvisor/pkg/tcpip"
)

func FromNetstackIP(s tcpip.Address) netip.Addr {
	switch s.Len() {
	case 4:
		s := s.As4()
		return netip.AddrFrom4([4]byte{s[0], s[1], s[2], s[3]})
	case 16:
		s := s.As16()
		return netip.AddrFrom16(s).Unmap()
	}
	return netip.Addr{}
}

func GetAddrPort(addr []byte, port uint16) (netip.AddrPort, error) {
	switch len(addr) {
	case 4:
		return netip.AddrPortFrom(netip.AddrFrom4([4]byte(addr)), port), nil
	case 16:
		return netip.AddrPortFrom(netip.AddrFrom16([16]byte(addr)), port), nil
	default:
		return netip.AddrPort{}, fmt.Errorf("unrecognize addr: %s", addr)
	}
}

func GenerateConnID(port int) uint32 {
	iterations := port * 2
	hash := sha256.Sum256(fmt.Appendf(nil, "%d", time.Now().UnixNano()))

	for i := 0; i < iterations; i++ {
		hash = sha256.Sum256(hash[:])
	}
	return binary.BigEndian.Uint32(hash[12:])
}
