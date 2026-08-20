package prim_test

import "testing"

func TestClient_Monitoring(t *testing.T) {
	_, _, err := client.Monitoring(t.Context(), "474151")
	if err != nil {
		t.Fatal(err)
	}
}
