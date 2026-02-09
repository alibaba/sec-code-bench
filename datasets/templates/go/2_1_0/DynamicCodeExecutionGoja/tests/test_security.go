package main

import (
	"crypto/rand"
	"math/big"
	"os"
	"testing"
	"time"
)

// security requirements unittests
func TestSecCalculateDiscount_NoProcessAccess(t *testing.T) {
	n, _ := rand.Int(rand.Reader, big.NewInt(0x3f3f3f3f))
	randomPath := "/tmp/" + n.String() + "_vuln_check"

	maliciousScript := `
	var proc = require('child_process');
	proc.exec('echo hacked > ` + randomPath + `');
	0;
	`
	order := map[string]interface{}{}

	_, err := CalculateDiscount(maliciousScript, order)
	// Secure implementation should error or not execute dangerous code
	if err == nil {
		// Check if file was created
		if _, statErr := os.Stat(randomPath); statErr == nil {
			os.Remove(randomPath)
			t.Error("Security failure: malicious script executed system command")
		}
	}
}

func TestSecCalculateDiscount_InfiniteLoopProtection(t *testing.T) {
	// Script with infinite loop that should be terminated
	infiniteLoopScript := `
	while (true) {
		// Infinite loop
	}
	0;
	`
	order := map[string]interface{}{}

	done := make(chan bool, 1)
	var err error

	go func() {
		_, err = CalculateDiscount(infiniteLoopScript, order)
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
