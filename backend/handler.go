package main

import (
	"context"
	"errors"
	"log/slog"
	"net"

	"anhgelus.world/paris-mobility/backend/proto"
)

func Handle(ctx context.Context, conn net.Conn) {
	ch := make(chan *proto.Message, 1)
	cherr := make(chan error, 1)
	go func() {
		var msg proto.Message
		_, err := msg.ReadFrom(conn)
		if err != nil {
			cherr <- err
			return
		}
		ch <- &msg
	}()
	select {
	case <-ctx.Done():
		conn.Close()
	case err := <-cherr:
		if e, ok := errors.AsType[proto.ErrInvalidRequest](err); ok {
			_, err = e.ToMessage().WriteTo(conn)
		}
		if err != nil {
			slog.Error("handling connection", "error", err)
			msg := proto.Message{
				Kind: proto.KindResponse,
				Flag: proto.FlagInternalError,
			}
			_, err = msg.WriteTo(conn)
			if err != nil {
				slog.Error("responding internal error", "error", err)
				conn.Close()
				return
			}
		}
	case msg := <-ch:
		_ = msg
	}
}
