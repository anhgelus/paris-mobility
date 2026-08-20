package prim_test

import (
	"bytes"
	"compress/gzip"
	"encoding/json"
	"testing"

	"anhgelus.world/go-cbor"
	"anhgelus.world/paris-mobility/backend/proto"
)

func TestClient_Disruptions(t *testing.T) {
	res, err := client.Disruptions(t.Context(), proto.DisruptionsRequest{})
	if err != nil {
		t.Fatal(err)
	}
	t.Log("length:", len(res))
	b, err := cbor.Marshal(res)
	if err != nil {
		t.Fatal("cannot marshal result:", err)
	}
	t.Log("size:", len(b))
	var buf bytes.Buffer
	w := gzip.NewWriter(&buf)
	w.Write(b)
	w.Close()
	t.Log("size (gzip): ", len(buf.Bytes()))
	b, _ = json.Marshal(res)
	t.Log("size (json):", len(b))
}
