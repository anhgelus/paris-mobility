package proto

type Status uint8

const (
	StatusOnTime Status = iota
	StatusEarly
	StatusDelayed
	StatusCancelled
	StatusMissed
	StatusArrived
	StatusDeparted
	StatusNotExpected
)

type Feature uint8

const (
	FeatureLongTrain Feature = iota
	FeatureShortTrain
)

type Monitoring map[string][]StopMonitoring

type StopMonitoring struct {
	Line           string    `cbor:"line"`
	IsStopped      bool      `cbor:"is_stopped"`
	Destination    []string  `cbor:"destination"`
	Time           uint64    `cbor:"time"`
	Status         Status    `cbor:"status"`
	VehicleFeature []Feature `cbor:"vehicle_feature"`
}
