package prim

import (
	"context"
	"encoding/json"
	"errors"
	"slices"
	"strings"
	"time"

	"anhgelus.world/paris-mobility/backend/proto"
)

type disruptions struct {
	Disruptions   []disruption   `json:"disruptions"`
	LinesAffected []lineAffected `json:"lines"`
}

type period struct {
	Begin time.Time
	End   time.Time
}

const primTimeFormat = `20060102T150405`

func (p *period) UnmarshalJSON(b []byte) error {
	var v struct {
		Begin *string `json:"begin"`
		End   *string `json:"end"`
	}
	err := json.Unmarshal(b, &v)
	if err != nil {
		return err
	}
	if v.Begin != nil {
		p.Begin, err = time.Parse(primTimeFormat, *v.Begin)
		if err != nil {
			return err
		}
	}
	if v.End != nil {
		p.End, err = time.Parse(primTimeFormat, *v.End)
		if err != nil {
			return err
		}
	}
	return nil
}

type disruption struct {
	Id               string    `json:"id"`
	Periods          []*period `json:"applicationPeriods"`
	Cause            string    `json:"cause"`
	Severity         string    `json:"severity"`
	Title            string    `json:"title"`
	Message          string    `json:"message"`
	ShortMessage     *string   `json:"shortMessage"`
	ImpactedSections []struct {
		Id string `json:"lineId"`
	} `json:"impactedSections"`
}

type lineAffected struct {
	Id              string   `json:"id"`
	Name            string   `json:"name"`
	Mode            string   `json:"mode"`
	ImpactedObjects []object `json:"impactedObjects"`
}

type object struct {
	Type          string   `json:"type"`
	Id            string   `json:"id"`
	Name          string   `json:"name"`
	DisruptionIds []string `json:"disruptionIds"`
}

func (c *Client) Disruptions(ctx context.Context, req proto.DisruptionsRequest) (proto.Disruptions, error) {
	var res disruptions
	err := c.do(ctx, "disruptions_bulk/disruptions/v2", &res)
	if err != nil {
		return nil, err
	}
	alreadyAdded := make(map[string]struct{})
	mp := make(map[string]proto.Disruption)
	for _, dis := range res.Disruptions {
		_, ok := alreadyAdded[dis.Title]
		if ok {
			continue
		}
		alreadyAdded[dis.Title] = struct{}{}
		cv := make([]proto.Period, 0, len(dis.Periods))
		for _, p := range dis.Periods {
			cv = append(cv, proto.Period{Begin: p.Begin.Unix(), End: p.End.Unix()})
		}
		var sev proto.DisruptionSeverity
		switch dis.Severity {
		case "INFORMATION":
			sev = proto.SeverityInformation
		case "PERTURBEE":
			sev = proto.SeverityPerturbe
		case "BLOQUANTE":
			sev = proto.SeverityBlocking
		default:
			return nil, errors.New("unkown disruption severity")
		}
		mp[dis.Id] = proto.Disruption{
			ID:           dis.Id,
			Periods:      cv,
			Severity:     sev,
			Title:        dis.Title,
			Cause:        dis.Cause,
			Message:      dis.Message,
			ShortMessage: dis.ShortMessage,
		}
	}
	dis := make(proto.Disruptions)
	for _, affected := range res.LinesAffected {
		//TODO: remove unwanted kinds
		id := slices.IndexFunc(affected.ImpactedObjects, func(obj object) bool {
			return obj.Type == "line"
		})
		if id < 0 {
			continue
		}
		line := affected.ImpactedObjects[id]
		acc := make([]proto.Disruption, 0, len(line.DisruptionIds))
		for _, rawId := range affected.ImpactedObjects[id].DisruptionIds {
			id := strings.Split(rawId, ":")[2]
			if len(req.Lines) > 0 && !slices.Contains(req.Lines, id) {
				continue
			}
			acc = append(acc, mp[id])
		}
		dis[affected.Id] = acc
	}
	return dis, nil
}
