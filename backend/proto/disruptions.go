package proto

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

type Disruptions map[string][]Disruption

type Period struct {
	Begin int64 `cbor:"begin"`
	End   int64 `cbor:"end"`
}

type Disruption struct {
	ID           string             `cbor:"id"`
	LineID       string             `cbor:"line_id"`
	Periods      []Period           `cbor:"periods"`
	Severity     DisruptionSeverity `cbor:"severity"`
	Cause        string             `cbor:"cause"`
	Title        string             `cbor:"title"`
	Message      string             `cbor:"message"`
	ShortMessage *string            `cbor:"short_message"`
}
