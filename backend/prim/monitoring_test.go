package prim_test

import (
	"bytes"
	"compress/gzip"
	"encoding/json"
	"testing"

	"anhgelus.world/go-cbor"
)

func TestClient_Monitoring(t *testing.T) {
	res, _, err := client.Monitoring(t.Context(), "474151")
	if err != nil {
		t.Fatal(err)
	}
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
