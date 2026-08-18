package proto

import (
	"bytes"
	"encoding/binary"
	"errors"
	"io"

	"anhgelus.world/go-cbor"
)

type Kind uint8

const (
	KindResponse Kind = iota
)

type Flag uint8

const (
	FlagDefault Flag = iota
	FlagInvalidRequest
	FlagInternalError
)

type Message struct {
	Kind Kind
	Flag Flag
	Body any
}

func (msg *Message) WriteTo(w io.Writer) (int64, error) {
	var buf bytes.Buffer
	buf.Grow(10)
	buf.WriteRune(rune(msg.Kind))
	buf.WriteRune(rune(msg.Flag))
	b, err := cbor.Marshal(msg.Body)
	if err != nil {
		return 0, err
	}
	ln := make([]byte, 0, 4)
	binary.BigEndian.PutUint32(ln, uint32(len(b)))
	buf.Write(ln)
	buf.WriteString("\r\n")
	buf.Write(b)
	buf.WriteString("\r\n")
	return buf.WriteTo(w)
}

func (msg *Message) ReadFrom(r io.Reader) (read int64, err error) {
	var header [8]byte
	n, err := r.Read(header[:])
	read = int64(n)
	if err != nil {
		return
	}
	msg.Kind = Kind(header[0])
	msg.Flag = Flag(header[1])
	ln := binary.BigEndian.Uint32(header[2:])
	body := make([]byte, 0, ln+2)
	n, err = r.Read(body)
	read += int64(n)
	if err != nil {
		return
	}
	rest, err := cbor.Unmarshal(body[:ln], msg.Body)
	if err != nil {
		return
	}
	if len(rest) != 0 {
		err = errors.New("CBOR contains more than one information")
		return
	}
	return
}
