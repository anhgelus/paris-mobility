package main

import (
	"context"

	"anhgelus.world/paris-mobility/backend/internal"
	"anhgelus.world/paris-mobility/backend/prim"
)

func PrimClient(ctx context.Context) *prim.Client {
	return ctx.Value(internal.KeyPrimClient).(*prim.Client)
}
