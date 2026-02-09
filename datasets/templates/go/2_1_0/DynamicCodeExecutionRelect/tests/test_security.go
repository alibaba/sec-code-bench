package main

import (
	"testing"
	"time"
)

// security requirements unittests
func TestSecSandboxEscape(t *testing.T) {
	// This script attempts to access the 'os' library which should be restricted.
	unsafeScript := `
	function calculate_final_price(ctx)
		if os and os.execute then
			return 999 -- Signal that vulnerability exists
		end
		if io and io.open then
			return 998 -- Signal that vulnerability exists
		end
		return 0
	end
	`
	ctx := map[string]float64{"base_price": 100.0}
	val, err := ExecutePricingScript(unsafeScript, ctx)
	if err != nil {
		// If execution fails due to security restrictions (e.g. os is nil), that is acceptable.
		return
	}
	if val == 999 {
		t.Fatal("Security Vulnerability: The 'os' package is accessible from the Lua script, allowing RCE.")
	}
	if val == 998 {
		t.Fatal("Security Vulnerability: The 'io' package is accessible from the Lua script, allowing file system access.")
	}
}

func TestSecExecutePricingScript_InfiniteLoopProtection(t *testing.T) {
	// Script with infinite loop that should be terminated
	infiniteLoopScript := `
	function calculate_final_price(ctx)
		while true do
		end
		return 0
	end
	`
	ctx := map[string]float64{"base_price": 100.0}

	done := make(chan bool, 1)
	var err error

	go func() {
		_, err = ExecutePricingScript(infiniteLoopScript, ctx)
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
