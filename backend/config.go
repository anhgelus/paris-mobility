package backend

import (
	"os"

	"github.com/BurntSushi/toml"
)

type Config struct {
	Token string `toml:"string"`
}

func ParseConfig(p string) (Config, error) {
	var cfg Config
	meta, err := toml.DecodeFile(p, &cfg)
	if os.IsNotExist(err) {
		err = os.WriteFile(p, nil, 0o600)
		if err != nil {
			return Config{}, err
		}
	}
	for _, k := range meta.Undecoded() {
		println(k.String())
	}
	return cfg, nil
}
