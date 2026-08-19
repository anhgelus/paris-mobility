package prim

import (
	"context"
	"encoding/json"
	"net/http"
	"net/url"
)

type Client struct {
	http.Client
	Endpoint string
	Token    string
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
	req.Header.Add("apiKey", c.Token)
	req.Header.Add("Accept", "application/json")
	resp, err := c.Do(req)
	if err != nil {
		return err
	}
	return json.NewDecoder(resp.Body).Decode(&v)
}
