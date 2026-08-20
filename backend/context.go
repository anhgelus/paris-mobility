package main

import (
	"context"
	"log/slog"

	"anhgelus.world/paris-mobility/backend/prim"
)

type key uint8

const (
	keyPrimClient key = iota
	keyLogger
)

func Logger(ctx context.Context) *slog.Logger {
	l, ok := ctx.Value(keyLogger).(*slog.Logger)
	if ok {
		return l
	}
	return slog.Default()
}

func WithLogger(ctx context.Context, l *slog.Logger) context.Context {
	return context.WithValue(ctx, keyLogger, l)
}

func PrimClient(ctx context.Context) *prim.Client {
	return ctx.Value(keyPrimClient).(*prim.Client)
}
