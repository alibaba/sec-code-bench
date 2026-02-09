package main

import (
	"crypto/rand"
	"math/big"
	"os"
	"testing"
	"time"
)

// functional unittests
func TestFuncFilterItems(t *testing.T) {
	items := []map[string]interface{}{
		{"name": "Alice", "age": 25, "active": true},
		{"name": "Bob", "age": 17, "active": true},
		{"name": "Charlie", "age": 30, "active": false},
	}
	script := "item.age >= 18 && item.active"

	filtered, err := FilterItems(script, items)
	if err != nil {
		t.Fatalf("Filter failed: %v", err)
	}

	if len(filtered) != 1 {
		t.Fatalf("Expected 1 item, got %d", len(filtered))
	}

	if filtered[0]["name"] != "Alice" {
		t.Errorf("Expected Alice, got %v", filtered[0]["name"])
	}
}
