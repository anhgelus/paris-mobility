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
}

func New() *Cache {
	return &Cache{
		disruptions:   make(proto.Disruptions),
		disruptionsCh: make(map[string]chan struct{}),
		DisruptionTO:  5 * time.Minute,
	}
}

func (c *Cache) Disruptions(req proto.DisruptionsRequest) (proto.Disruptions, bool) {
	c.disMu.RLock()
	defer c.disMu.RUnlock()
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
	c.disMu.Lock()
	defer c.disMu.Unlock()
	for line, content := range dis {
		ch, ok := c.disruptionsCh[line]
		if ok {
			close(ch)
		}
		c.disruptions[line] = content
		c.disruptionsCh[line] = make(chan struct{})
	}
	go func() {
		<-time.Tick(c.DisruptionTO)
		c.disMu.Lock()
		defer c.disMu.Unlock()
		for line := range dis {
			select {
			case <-c.disruptionsCh[line]:
				continue
			default:
			}
			delete(c.disruptions, line)
			delete(c.disruptionsCh, line)
		}
	}()
}
