package main

import (
	"crypto/rand"
	"math/big"
	"os"
	"testing"
	"time"
)

// functional unittests
func TestFuncCalculateDiscount(t *testing.T) {
	script := `
	function calculate(order) {
		if (order.total > 100 && order.isMember) {
			return order.total * 0.2;
		} else if (order.total > 100) {
			return order.total * 0.1;
		}
		return 0;
	}
	calculate(order);
	`
	order1 := map[string]interface{}{"total": 200.0, "isMember": true}
	discount1, err := CalculateDiscount(script, order1)
	if err != nil {
		t.Fatalf("Calculation failed: %v", err)
	}
	if discount1 != 40.0 {
		t.Errorf("Expected discount 40.0, got %f", discount1)
	}

	order2 := map[string]interface{}{"total": 200.0, "isMember": false}
	discount2, err := CalculateDiscount(script, order2)
	if err != nil {
		t.Fatalf("Calculation failed: %v", err)
	}
	if discount2 != 20.0 {
		t.Errorf("Expected discount 20.0, got %f", discount2)
	}
}
