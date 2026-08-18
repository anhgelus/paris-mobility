package proto

import (
	"bytes"
	"encoding/binary"
	"io"

	"anhgelus.world/go-cbor"
)

type Kind uint8

const (
	KindResponse Kind = iota
	KindInvalidRequest
	KindInternalError
	KindDisruptions
	KindMonitoring
	KindGoodbye
)

type Flag uint8

const () // currently, there is no flag

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

type ErrInvalidRequest struct {
	Reason string
	Err    error
}

func (err ErrInvalidRequest) Error() string {
	return err.Reason
}

func (err ErrInvalidRequest) Unwrap() error {
	return err.Err
}

func (err ErrInvalidRequest) ToMessage() *Message {
	var e string
	if err.Err != nil {
		e = err.Err.Error()
	}
	return &Message{
		Kind: KindInvalidRequest,
		Body: struct {
			Message string `cbor:"message"`
			Error   string `cbor:"error,omitzero"`
		}{err.Reason, e},
	}
}

func (msg *Message) ReadFrom(r io.Reader) (read int64, err error) {
	var header [8]byte
	n, err := r.Read(header[:])
	read = int64(n)
	if err != nil {
		return
	}
	if n != len(header) {
		err = ErrInvalidRequest{Reason: "content malformed"}
		return
	}
	msg.Kind = Kind(header[0])
	msg.Flag = Flag(header[1])
	ln := binary.BigEndian.Uint32(header[2:])
	rawBody := make([]byte, 0, ln+2)
	n, err = r.Read(rawBody)
	read += int64(n)
	if err != nil {
		return
	}
	if uint32(n) != ln+2 {
		err = ErrInvalidRequest{Reason: "content malformed"}
		return
	}
	var rest []byte
	var body any
	switch msg.Kind {
	case KindDisruptions:
		body = DisruptionsRequest{}
	case KindMonitoring:
		body = MonitoringRequest{}
	case KindGoodbye:
	default:
		err = ErrInvalidRequest{Reason: "invalid kind"}
		return
	}
	rest, err = cbor.Unmarshal(rawBody[:ln], &body)
	msg.Body = body
	if err != nil {
		err = ErrInvalidRequest{Reason: "content malformed", Err: err}
		return
	}
	if len(rest) != 0 {
		err = ErrInvalidRequest{Reason: "contains more than one CBOR"}
	}
	return
}
