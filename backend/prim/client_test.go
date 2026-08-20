package prim_test

import (
	"net/http"
	"os"

	"anhgelus.world/paris-mobility/backend/cache"
	"anhgelus.world/paris-mobility/backend/prim"
)

var client = prim.New(
	"https://prim.iledefrance-mobilites.fr/marketplace",
	os.Getenv("PRIM_TOKEN"),
	http.DefaultClient,
	cache.New(),
)
