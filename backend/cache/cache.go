package cache

import (
	"sync"
	"time"

	"anhgelus.world/paris-mobility/backend/proto"
)

type Cache struct {
	disruptions   proto.Disruptions
	disruptionsCh map[string]chan struct{}
	disMu         sync.RWMutex
	DisruptionTO  time.Duration

	stops   map[string]map[string][]proto.StopMonitoring
	stopsCh map[string]chan struct{}
	stopsMu sync.RWMutex
	StopsTO time.Duration
}

func New() *Cache {
	return &Cache{
		disruptions:   make(proto.Disruptions),
		disruptionsCh: make(map[string]chan struct{}),
		DisruptionTO:  5 * time.Minute,
		stops:         make(map[string]map[string][]proto.StopMonitoring),
		stopsCh:       make(map[string]chan struct{}),
		StopsTO:       2 * time.Minute,
	}
}

func (c *Cache) Disruptions(req proto.DisruptionsRequest) (proto.Disruptions, bool) {
	c.disMu.RLock()
	defer c.disMu.RUnlock()
	if len(req.Lines) == 0 {
		return nil, false
	}
	dis := make(proto.Disruptions)
	for _, line := range req.Lines {
		v, ok := c.disruptions[line]
		if !ok {
			return nil, false
		}
		dis[line] = v
	}
	return dis, true
}

func (c *Cache) UpdateDisruptions(dis proto.Disruptions) {
	update(&c.disMu, c.DisruptionTO, c.disruptions, c.disruptionsCh, dis)
}

func (c *Cache) Stop(zda string) (map[string][]proto.StopMonitoring, bool) {
	c.stopsMu.RLock()
	defer c.stopsMu.RUnlock()
	v, ok := c.stops[zda]
	return v, ok
}

func (c *Cache) UpdateStops(stops map[string]map[string][]proto.StopMonitoring) {
	update(&c.stopsMu, c.StopsTO, c.stops, c.stopsCh, stops)
}

func update[K comparable, V any](mu *sync.RWMutex, to time.Duration, mp map[K]V, mpCh map[K]chan struct{}, new map[K]V) {
	mu.Lock()
	defer mu.Unlock()
	for k, content := range new {
		ch, ok := mpCh[k]
		if ok {
			close(ch)
		}
		mp[k] = content
		mpCh[k] = make(chan struct{})
	}
	go func() {
		<-time.After(to)
		mu.Lock()
		defer mu.Unlock()
		for key := range new {
			select {
			case <-mpCh[key]:
				continue
			default:
			}
			delete(mp, key)
			delete(mpCh, key)
		}
	}()
}
