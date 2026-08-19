package main

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"net"
	"time"

	"anhgelus.world/paris-mobility/backend/proto"
)

func Handle(ctx context.Context, conn net.Conn) {
	ch := make(chan any, 1)
	for {
		go func() {
			var msg proto.Message
			_, err := msg.ReadFrom(conn)
			if err != nil {
				ch <- err
				return
			}
			if msg.Kind == proto.KindGoodbye {
				close(ch)
				return
			}
			ch <- msg.Body
		}()
		select {
		case <-ctx.Done():
			msg := &proto.Message{
				Kind: proto.KindGoodbye,
				Body: "",
			}
			err := conn.SetWriteDeadline(time.Now().Add(2 * time.Second))
			if err == nil {
				_, _ = msg.WriteTo(conn)
			}
			conn.Close()
			return
		case got, ok := <-ch:
			if !ok {
				conn.Close()
				return
			}
			var msg *proto.Message
			var err error
			switch v := got.(type) {
			case proto.DisruptionsRequest:
				msg, err = handleDisruptions(ctx, v)
			case proto.MonitoringRequest:
				msg, err = handleMonitoring(ctx, v)
			case error:
				err = v
			default:
				panic("unknown value type: " + fmt.Sprintf("%T", v))
			}
			if e, ok := errors.AsType[proto.ErrInvalidRequest](err); ok {
				msg = e.ToMessage()
				err = nil
			}
			if err == nil {
				_, err = msg.WriteTo(conn)
			}
			if err != nil {
				slog.Error("handling message", "error", err)
				msg := proto.Message{
					Kind: proto.KindInternalError,
					Body: "internal error",
				}
				_, err = msg.WriteTo(conn)
				if err != nil {
					slog.Error("responding internal error", "error", err)
					conn.Close()
					return
				}
			}
		}
	}
}

func handleDisruptions(ctx context.Context, req proto.DisruptionsRequest) (*proto.Message, error) {
	dis, err := PrimClient(ctx).Disruptions(ctx, req)
	if err != nil {
		return nil, err
	}
	return &proto.Message{
		Kind: proto.KindResponse,
		Body: dis,
	}, nil
}

func handleMonitoring(ctx context.Context, req proto.MonitoringRequest) (*proto.Message, error) {
	resp := make(proto.Monitoring, len(req.Stops))
	return &proto.Message{
		Kind: proto.KindResponse,
		Body: resp,
	}, nil
}
