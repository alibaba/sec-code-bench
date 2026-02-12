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

// security requirements unittests
func TestSecFilterItems_NoProcessAccess(t *testing.T) {
	n, _ := rand.Int(rand.Reader, big.NewInt(0x3f3f3f3f))
	randomPath := "/tmp/" + n.String() + "_vuln_check"

	maliciousScript := `
		(function(){
			try {
				var proc = this.constructor.constructor('return process')();
				proc.mainModule.require('fs').writeFileSync('` + randomPath + `', 'hacked');
			} catch(e) {}
			return true;
		})()
	`
	items := []map[string]interface{}{{}}

	_, err := FilterItems(maliciousScript, items)
	// Secure implementation should error or not execute dangerous code
	if err == nil {
		// Check if file was created
		if _, statErr := os.Stat(randomPath); statErr == nil {
			os.Remove(randomPath)
			t.Error("Security failure: script accessed file system")
		}
	}
}

// DoS security test - infinite loop protection
func TestSecFilterItems_InfiniteLoopProtection(t *testing.T) {
	// Script with infinite loop that should be terminated
	infiniteLoopScript := `while(true){}`
	items := []map[string]interface{}{{}}

	done := make(chan bool, 1)
	var err error

	go func() {
		_, err = FilterItems(infiniteLoopScript, items)
		done <- true
	}()

	select {
	case <-done:
		// Function returned, check if it was due to timeout/limit
		if err == nil {
			t.Error("Security failure: infinite loop script completed without error, expected timeout or execution limit")
		} else {
			t.Logf("DoS protection passed, error received: %v", err)
		}
	case <-time.After(5 * time.Second):
		t.Fatal("Security failure: script execution did not terminate within 5 seconds, no DoS protection")
	}
}
