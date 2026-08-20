package main

import (
	"context"
	_ "embed"
	"flag"
	"log/slog"
	"log/syslog"
	"net/http"
	"os"
	"os/signal"
	"syscall"

	"anhgelus.world/paris-mobility/backend/cache"
	"anhgelus.world/paris-mobility/backend/prim"
	"github.com/nyttikord/logos"
)

var (
	config   string = "/etc/paris-mobility.toml"
	verbose  bool
	toSyslog bool
)

//go:embed default.toml
var defaultConfig []byte

func init() {
	flag.StringVar(&config, "config", config, "sets the config path")
	flag.BoolVar(&verbose, "v", verbose, "increase verbosity")
	flag.BoolVar(&toSyslog, "syslog", toSyslog, "log to syslog")
}

func main() {
	flag.Parse()
	var h slog.Handler
	level := slog.LevelInfo
	if verbose {
		level = slog.LevelDebug
	}
	if toSyslog {
		var err error
		h, err = logos.NewSyslog("paris-mobility", syslog.LOG_USER, &logos.Options{
			Level: level,
		})
		if err != nil {
			panic(err)
		}
	} else {
		h = logos.NewColor(os.Stderr, &logos.Options{
			Level: level,
		})
	}
	slog.SetDefault(slog.New(h))
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
	ctx = context.WithValue(ctx, keyPrimClient, prim.New(
		"https://prim.iledefrance-mobilites.fr/marketplace",
		cfg.Token,
		http.DefaultClient,
		cache.New(),
	))
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
				}
				lg.Error("accepting connection", "error", err)
				continue
			}
			go Handle(WithLogger(ctx, lg.With("ip", conn.RemoteAddr())), conn)
		}
	}()
	<-ctx.Done()
	slog.Info("exiting")
}
