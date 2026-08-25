package proto

import (
	"bytes"
	"compress/gzip"
	"encoding/binary"
	"errors"
	"fmt"
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
)

type Flag uint8

const (
	FlagGZipped Flag = 1 << iota
)

type Message struct {
	Kind Kind
	Flag Flag
	Body any
}

func (msg *Message) WriteTo(w io.Writer) (int64, error) {
	var buf bytes.Buffer
	buf.Grow(8)
	buf.WriteRune(rune(msg.Kind))
	b, err := cbor.Marshal(msg.Body)
	if err != nil {
		return 0, err
	}
	ln := make([]byte, 4)
	if len(b) > 512 {
		msg.Flag |= FlagGZipped
		var buf bytes.Buffer
		w := gzip.NewWriter(&buf)
		_, err := w.Write(b)
		if err != nil {
			return 0, err
		}
		w.Close()
		b, _ = io.ReadAll(&buf)
	}
	buf.WriteRune(rune(msg.Flag))
	binary.BigEndian.PutUint32(ln, uint32(len(b)))
	buf.Write(ln)
	buf.WriteString("\r\n")
	buf.Write(b)
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
	_, err = io.ReadFull(r, header[:])
	if err != nil {
		if errors.Is(err, io.ErrUnexpectedEOF) {
			err = ErrInvalidRequest{Reason: "content malformed"}
		}
		return
	}
	msg.Kind = Kind(header[0])
	msg.Flag = Flag(header[1])
	ln := binary.BigEndian.Uint32(header[2:])
	rawBody := make([]byte, ln)
	_, err = io.ReadFull(r, rawBody)
	if err != nil {
		if errors.Is(err, io.ErrUnexpectedEOF) {
			err = ErrInvalidRequest{Reason: "content malformed"}
		}
		return
	}
	var rest []byte
	var body any
	switch msg.Kind {
	case KindDisruptions:
		var v DisruptionsRequest
		rest, err = unmarshal(rawBody, &v)
		body = v
	case KindMonitoring:
		var v MonitoringRequest
		rest, err = unmarshal(rawBody, &v)
		body = v
	default:
		err = ErrInvalidRequest{Reason: "invalid kind"}
		return
	}
	if err != nil {
		fmt.Printf("% x\n", rawBody)
		err = ErrInvalidRequest{Reason: "content malformed", Err: err}
		return
	}
	if len(rest) != 0 {
		fmt.Printf("%v (%T)\n", body, body)
		err = ErrInvalidRequest{Reason: "contains more than one CBOR"}
	}
	msg.Body = body
	return
}

func unmarshal[T any](b []byte, v *T) ([]byte, error) {
	return cbor.Unmarshal(b, &v)
}
