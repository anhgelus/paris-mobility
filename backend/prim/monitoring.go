package prim

import (
	"context"
	"errors"
	"time"

	"anhgelus.world/paris-mobility/backend/proto"
)

type ref struct {
	Value string `json:"value"`
}

type stop struct {
	RecordedAt string `json:"RecordedAtTime"`
	Ref        ref
	Journey    journey `json:"MonitoredVehicleJourney"`
}

type journey struct {
	LineRef        ref
	DirectionName  []ref
	Monitored      monitored `json:"MonitoredCall"`
	VehicleFeature *string   `json:"VehicleFeatureRef"`
}

type monitored struct {
	IsStopped          bool `json:"VehicleAtStop"`
	DestinationDisplay []ref
	ArrivalTime        *string `json:"ExpectedArrivalTime"`
	DepartureTime      *string `json:"ExpectedDepartureTime"`
	Status             string  `json:"DepartureStatus"`
}

func (c *Client) Monitoring(ctx context.Context, zda string) (map[string]proto.StopMonitoring, bool, error) {
	got, ok := c.Cache.Stop(zda)
	if ok {
		return got, false, nil
	}
	var v struct {
		Siri struct {
			ServiceDelivery struct {
				StopMonitoringDelivery []struct {
					Stops []stop `json:"MonitoredStopVisit"`
				}
			}
		}
	}
	err := c.do(ctx, "stop-monitoring?MonitoringRef=STIF%3AStopArea%3ASP%3A"+zda+"%3A", &v)
	if err != nil {
		return nil, false, err
	}
	if len(v.Siri.ServiceDelivery.StopMonitoringDelivery) == 0 {
		return nil, false, proto.ErrInvalidRequest{Reason: "stop not found"}
	}
	visits := v.Siri.ServiceDelivery.StopMonitoringDelivery[0].Stops
	res := make(map[string]proto.StopMonitoring, len(visits))
	for _, journey := range visits {
		stop := journey.Journey
		if stop.Monitored.ArrivalTime == nil && stop.Monitored.DepartureTime == nil {
			return nil, false, errors.New("arrival time and departure time are absent")
		}
		var t time.Time
		if stop.Monitored.ArrivalTime != nil {
			t, err = time.Parse(time.RFC1123, *stop.Monitored.ArrivalTime)
		} else {
			t, err = time.Parse(time.RFC1123, *stop.Monitored.DepartureTime)
		}
		if err != nil {
			return nil, false, err
		}
		if t.Before(time.Now()) {
			continue
		}
		dest := make([]string, 0, len(stop.DirectionName))
		for _, s := range stop.DirectionName {
			dest = append(dest, s.Value)
		}
		var f *proto.Feature
		if stop.VehicleFeature != nil {
			switch *stop.VehicleFeature {
			case "shortTrain":
				f = new(proto.FeatureShortTrain)
			case "longTrain":
				f = new(proto.FeatureLongTrain)
			default:
				return nil, false, errors.New("unknown vehicle feature: " + *stop.VehicleFeature)
			}
		}
		var status proto.Status
		switch stop.Monitored.Status {
		case "onTime":
			status = proto.StatusOnTime
		case "early":
			status = proto.StatusEarly
		case "delayed":
			status = proto.StatusDelayed
		case "cancelled":
			status = proto.StatusCancelled
		case "missed":
			status = proto.StatusMissed
		case "arrived":
			status = proto.StatusArrived
		case "departed":
			status = proto.StatusDeparted
		case "notExpected":
			status = proto.StatusNotExpected
		default:
			return nil, false, errors.New("unknown status: " + stop.Monitored.Status)
		}
		res[stop.DirectionName[0].Value] = proto.StopMonitoring{
			IsStopped:      stop.Monitored.IsStopped,
			Destination:    dest,
			Time:           uint64(t.Unix()),
			Status:         status,
			VehicleFeature: f,
		}
	}
	return res, true, nil
}
