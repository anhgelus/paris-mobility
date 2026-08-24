package internal

import (
	"context"
	"log/slog"
)

type Key uint8

const (
	KeyPrimClient Key = iota
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
