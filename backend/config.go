package main

import (
	"errors"
	"net"
	"os"
	"os/user"
	"strconv"
	"strings"

	"github.com/BurntSushi/toml"
	"github.com/pires/go-proxyproto"
)

type Config struct {
	Token            string `toml:"token"`
	ListenAddr       string `toml:"listen"`
	UseProxyV2       bool   `toml:"use_proxy_v2"`
	SocketGroup      any    `toml:"socket_group"`
	SocketPermission uint   `toml:"socket_permission"`
}

func ParseConfig(p string) (Config, error) {
	var cfg Config
	meta, err := toml.DecodeFile(p, &cfg)
	if err != nil {
		return cfg, err
	}
	if !meta.IsDefined("listen") {
		return cfg, errors.New("listen address not set")
	}
	if !meta.IsDefined("token") {
		return cfg, errors.New("PRIM token not set")
	}
	for _, k := range meta.Undecoded() {
		println(k.String())
	}
	return cfg, nil
}

func (cfg Config) Listen() (l net.Listener, err error) {
	kind := "tcp"
	if strings.ContainsAny(cfg.ListenAddr, "/") {
		kind = "unix"
	}
	l, err = net.Listen(kind, cfg.ListenAddr)
	if err != nil {
		return nil, err
	}
	if cfg.UseProxyV2 {
		l = &proxyproto.Listener{Listener: l}
	}
	defer func() {
		if err != nil {
			l.Close()
		}
	}()
	if kind == "unix" {
		if cfg.SocketPermission > 0 {
			err = os.Chmod(cfg.ListenAddr, os.FileMode(cfg.SocketPermission))
			if err != nil {
				return
			}
		}
		if cfg.SocketGroup != nil {
			var gid int
			switch v := cfg.SocketGroup.(type) {
			case int64:
				if gid < 0 {
					err = errors.New("invalid socket group: must be an uint")
					return
				}
				gid = int(v)
			case string:
				group, err := user.LookupGroup(v)
				if err != nil {
					return nil, err
				}
				gid, _ = strconv.Atoi(group.Gid)
			default:
				err = errors.New("invalid socket group type: must be an uint or a string")
				return
			}
			err = os.Chown(cfg.ListenAddr, -1, gid)
			if err != nil {
				return
			}
		}
	}
	return
}
