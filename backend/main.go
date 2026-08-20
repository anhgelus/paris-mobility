package main

import (
	"context"
	_ "embed"
	"flag"
	"log/slog"
	"os"
	"os/signal"
	"syscall"
)

var (
	config  string = "/etc/paris-mobility.toml"
	verbose bool
)

//go:embed default.toml
var defaultConfig []byte

func init() {
	flag.StringVar(&config, "config", config, "sets the config path")
	flag.BoolVar(&verbose, "v", verbose, "increase verbosity")
}

func main() {
	flag.Parse()
	cfg, err := ParseConfig(config)
	if err != nil {
		if !os.IsNotExist(err) {
			slog.Error("reading config", "error", err, "path", config)
			os.Exit(1)
		}
		slog.Warn("config file not found, writing default one", "path", config)
		err = os.WriteFile(config, defaultConfig, 0o600)
		if err != nil {
			slog.Error("writing config file", "path", config, "error", err)
		}
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
		lg := Logger(ctx)
		for {
			conn, err := l.Accept()
			if err != nil {
				select {
				case <-ctx.Done():
					return
				default:
					lg.Error("accepting request", "error", err)
					continue
				}
			}
			go Handle(WithLogger(ctx, lg.With("ip", conn.RemoteAddr())), conn)
		}
	}()
	<-ctx.Done()
	slog.Info("exiting")
}
