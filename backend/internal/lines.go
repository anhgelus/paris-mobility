package internal

import (
	"fmt"
	"io"
	"text/template"
)

type TransportMode string

const (
	BusMode           TransportMode = "bus"
	RailMode          TransportMode = "rail"
	FunicularMode     TransportMode = "funicular"
	MetroMode         TransportMode = "metro"
	TramMode          TransportMode = "tram"
	CablewayMode      TransportMode = "cableway"
	WaterMode         TransportMode = "water"
	RERSubmode        TransportMode = RailMode + ":local"
	TERSubmode        TransportMode = RailMode + ":regionalRail"
	TransilienSubmode TransportMode = RailMode + ":suburbanRailway"
)

func HasSubMode(mode TransportMode) bool {
	switch mode {
	case BusMode, RailMode:
		return true
	default:
		return false
	}
}

type TransportStatus string

const (
	ActiveStatus   TransportStatus = "active"
	InactiveStatus TransportStatus = "prochainement active"
)

type Line struct {
	Id             string           `json:"id_line"`
	Name           string           `json:"name_line"`
	ShortName      *string          `json:"shortname_line"`
	Mode           TransportMode    `json:"transportmode"`
	Submode        *TransportMode   `json:"transportsubmode"`
	IdGroupOfLines *string          `json:"id_groupoflines"`
	NetworkName    *string          `json:"networkname"`
	Status         *TransportStatus `json:"status"`
}

var parse = template.Must(template.New("").Funcs(template.FuncMap{
	"nilOrNew":       nilOrNew[string],
	"nilOrNewMode":   nilOrNew[TransportMode],
	"nilOrNewStatus": nilOrNew[TransportStatus],
}).Parse(`
var {{ .Id }} = &internal.Line{
	Id: "{{ .Id }}",
	Name: "{{ .Name }}",
	ShortName: {{ nilOrNew .ShortName }},
	Mode: "{{ .Mode }}",
	Submode: {{ nilOrNewMode .Submode }},
	IdGroupOfLines: {{ nilOrNew .IdGroupOfLines }},
	NetworkName: {{ nilOrNew .NetworkName }},
	Status: {{ nilOrNewStatus .Status }},
}
`))

func nilOrNew[T ~string](s *T) string {
	if s == nil {
		return "nil"
	}
	return fmt.Sprintf(`new(%T("%s"))`, *s, *s)
}

func (l Line) Generate(w io.Writer) {
	err := parse.Execute(w, l)
	if err != nil {
		panic(err)
	}
}
