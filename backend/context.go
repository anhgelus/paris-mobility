package main

import (
	"context"

	"anhgelus.world/paris-mobility/backend/prim"
)

type key uint8

const (
	KeyPrimClient key = iota
)

func PrimClient(ctx context.Context) *prim.Client {
	return ctx.Value(KeyPrimClient).(*prim.Client)
}
