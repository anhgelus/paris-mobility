package proto

type Period struct {
	Begin uint64 `cbor:"begin"`
	End   uint64 `cbor:"end"`
}

type DisruptionSeverity uint8

const (
	SeverityInformation DisruptionSeverity = iota
	SeverityPerturbe
	SeverityBlocking
)

type TransportMode uint8

const (
	TransportRER TransportMode = iota
	TransportMetro
	TransportTram
	TransportTransilien
)

type Disruptions map[TransportMode][]Disruption

type Disruption struct {
	ID           string             `cbor:"id"`
	LineID       string             `cbor:"line_id"`
	Periods      uint64             `cbor:"periods"`
	Severity     DisruptionSeverity `cbor:"severity"`
	Cause        string             `cbor:"cause"`
	Title        string             `cbor:"title"`
	Message      string             `cbor:"message"`
	ShortMessage *string            `cbor:"short_message"`
}
