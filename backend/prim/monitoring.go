package prim

import (
	"context"
	"errors"
	"slices"
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
	VehicleFeature []string  `json:"VehicleFeatureRef"`
}

type monitored struct {
	IsStopped          bool `json:"VehicleAtStop"`
	DestinationDisplay []ref
	ArrivalTime        *string `json:"ExpectedArrivalTime"`
	DepartureTime      *string `json:"ExpectedDepartureTime"`
	Status             string  `json:"DepartureStatus"`
}

func (c *Client) Monitoring(ctx context.Context, zda string) ([]proto.StopMonitoring, bool, error) {
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
	res := make([]proto.StopMonitoring, 0, len(visits))
	for _, stop := range visits {
		journey := stop.Journey
		if journey.Monitored.ArrivalTime == nil && journey.Monitored.DepartureTime == nil {
			return nil, false, errors.New("arrival time and departure time are absent")
		}
		var t time.Time
		if journey.Monitored.ArrivalTime != nil {
			t, err = time.Parse(time.RFC3339, *journey.Monitored.ArrivalTime)
		} else {
			t, err = time.Parse(time.RFC3339, *journey.Monitored.DepartureTime)
		}
		if err != nil {
			return nil, false, err
		}
		if t.Before(time.Now()) {
			continue
		}
		dest := make([]string, 0, len(journey.DirectionName))
		for _, s := range journey.DirectionName {
			dest = append(dest, s.Value)
		}
		var fs []proto.Feature
		for _, f := range journey.VehicleFeature {
			switch f {
			case "shortTrain":
				fs = append(fs, proto.FeatureShortTrain)
			case "longTrain":
				fs = append(fs, proto.FeatureLongTrain)
			default:
				return nil, false, errors.New("unknown vehicle feature: " + f)
			}
		}
		var status proto.Status
		switch journey.Monitored.Status {
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
			return nil, false, errors.New("unknown status: " + journey.Monitored.Status)
		}
		res = append(res, proto.StopMonitoring{
			IsStopped:      journey.Monitored.IsStopped,
			Destination:    dest,
			Time:           uint64(t.Unix()),
			Status:         status,
			VehicleFeature: fs,
		})
	}
	slices.SortFunc(res, func(a, b proto.StopMonitoring) int {
		return int(int64(a.Time) - int64(b.Time))
	})
	return res, true, nil
}
