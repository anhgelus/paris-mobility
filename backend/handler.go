package main

import (
	"context"
	"errors"
	"fmt"
	"net"
	"time"

	"anhgelus.world/paris-mobility/backend/proto"
)

func Handle(ctx context.Context, conn net.Conn) {
	ch := make(chan any, 1)
	l := Logger(ctx)
	l.Debug("new connection")
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
			msg := &proto.Message{Kind: proto.KindGoodbye}
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
			sub, cancel := context.WithTimeout(ctx, 10*time.Second)
			switch v := got.(type) {
			case proto.DisruptionsRequest:
				l = l.With("kind", "disruptions")
				msg, err = handleDisruptions(WithLogger(sub, l), v)
			case proto.MonitoringRequest:
				l = l.With("kind", "monitoring")
				msg, err = handleMonitoring(WithLogger(sub, l), v)
			case error:
				err = v
			default:
				panic("unknown value type: " + fmt.Sprintf("%T", v))
			}
			cancel()
			if e, ok := errors.AsType[proto.ErrInvalidRequest](err); ok {
				msg = e.ToMessage()
				err = nil
			}
			if err == nil {
				_, err = msg.WriteTo(conn)
			}
			if err == nil {
				continue
			}
			l.Error("handling message", "error", err)
			msg = &proto.Message{
				Kind: proto.KindInternalError,
				Body: "internal error",
			}
			err = conn.SetWriteDeadline(time.Now().Add(2 * time.Second))
			if err == nil {
				_, err = msg.WriteTo(conn)
			}
			if err != nil {
				l.Error("responding internal error", "error", err)
				l.Warn("closing connection")
				conn.Close()
				return
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
	acc := make(map[string]map[string][]proto.StopMonitoring, len(req.Stops))
	cached := make(map[string]map[string][]proto.StopMonitoring)
	cl := PrimClient(ctx)
	for _, stop := range req.Stops {
		monitors, toCache, err := cl.Monitoring(ctx, stop)
		if err != nil {
			return nil, err
		}
		acc[stop] = monitors
		if toCache {
			cached[stop] = monitors
		}
	}
	cl.Cache.UpdateStops(cached)
	return &proto.Message{
		Kind: proto.KindResponse,
		Body: acc,
	}, nil
}
