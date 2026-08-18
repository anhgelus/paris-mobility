package proto

type DisruptionsRequest struct {
	Kinds []TransportMode `cbor:"kinds"`
	Lines []string        `cbor:"lines"`
}

type MonitoringRequest struct {
	Stops []string `cbor:"stops"`
}
