package prim

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"

	"anhgelus.world/paris-mobility/backend/cache"
)

type Client struct {
	*http.Client
	Endpoint string
	token    string
	Cache    *cache.Cache
}

func New(endpoint, token string, client *http.Client, cache *cache.Cache) *Client {
	return &Client{
		Client:   client,
		Endpoint: endpoint,
		token:    token,
		Cache:    cache,
	}
}

func (c *Client) do(ctx context.Context, t string, v any) error {
	target, err := url.JoinPath(c.Endpoint, t)
	if err != nil {
		return err
	}
	req, err := http.NewRequest(http.MethodGet, target, nil)
	if err != nil {
		return err
	}
	req = req.WithContext(ctx)
	req.Header["apiKey"] = []string{c.token}
	req.Header.Add("Accept", "application/json")
	req.Header.Add("User-Agent", "ParisMobility/1.0.0")
	resp, err := c.Do(req)
	if err != nil {
		return err
	}
	if resp.StatusCode > 399 {
		return fmt.Errorf("invalid status code: %d", resp.StatusCode)
	}
	return json.NewDecoder(resp.Body).Decode(&v)
}
