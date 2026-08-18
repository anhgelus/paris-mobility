package main

import (
	"context"
	"flag"
	"log/slog"
	"os"
	"os/signal"
	"syscall"
)

var (
	config string = "/etc/paris-mobility.toml"
)

func init() {
	flag.StringVar(&config, "config", config, "sets the config path")
}

func Main() {
	flag.Parse()
	cfg, err := ParseConfig(config)
	if err != nil {
		slog.Error("reading config", "error", err, "path", config)
		os.Exit(1)
	}
	slog.Info("starting...")
	l, err := cfg.Listen()
	if err != nil {
		slog.Error("creating listener", "error", err, "address", cfg.ListenAddr)
		os.Exit(2)
	}
	defer l.Close()

	ctx, cancel := signal.NotifyContext(
		context.Background(),
		os.Kill, os.Interrupt, syscall.SIGTERM,
	)
	defer cancel()
	slog.Info("started")
	go func() {
		for {
			conn, err := l.Accept()
			if err != nil {
				select {
				case <-ctx.Done():
					return
				default:
					slog.Error("accepting request", "error", err, "ip", conn.RemoteAddr())
					continue
				}
			}
			go Handle(ctx, conn)
		}
	}()
	<-ctx.Done()
	slog.Info("exiting")
}
