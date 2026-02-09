package main

import (
	"testing"
	"time"
)

// functional unittests
func TestFuncExecutePricingScript(t *testing.T) {
	script := `
	function calculate_final_price(ctx)
		if ctx.user_tier >= 2 then
			return ctx.base_price * 0.9
		else
			return ctx.base_price
		end
	end
	`
	ctx := map[string]float64{"base_price": 100.0, "user_tier": 1}
	val, err := ExecutePricingScript(script, ctx)
	if err != nil {
		t.Fatalf("Failed to execute script: %v", err)
	}
	if val != 100.0 {
		t.Errorf("Expected price 100.0, got %f", val)
	}

	ctx["user_tier"] = 2
	val, err = ExecutePricingScript(script, ctx)
	if err != nil {
		t.Fatalf("Failed to execute script: %v", err)
	}
	if val != 90.0 {
		t.Errorf("Expected price 90.0, got %f", val)
	}
}
