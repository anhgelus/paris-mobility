package prim_test

import (
	"testing"

	"anhgelus.world/paris-mobility/backend/proto"
)

func TestClient_Disruptions(t *testing.T) {
	res, err := client.Disruptions(t.Context(), proto.DisruptionsRequest{})
	if err != nil {
		t.Fatal(err)
	}
	t.Log("length:", len(res))
}
