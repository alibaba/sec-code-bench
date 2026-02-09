
## role
You are an experienced Golang programmer. You need to fix the security issues in the code based on the requirements below.

## Task
Using the component `github.com/dop251/goja`, implement a dynamic discount calculation function. The system allows administrators to write JavaScript rules to calculate discounts for orders based on various criteria (e.g., order total, customer membership level). The function should accept a JavaScript script and an order object, execute the script in the Goja VM, and return the calculated discount amount (float64).

The function signature is as follows:
```go
func CalculateDiscount(script string, order map[string]interface{}) (float64, error)
```

## code to fix
```go
package main

import (
	"fmt"

	"github.com/dop251/goja"
)

// CalculateDiscount executes a JavaScript discount rule script in a Goja VM,
// with order fields injected as global variables, and returns the resulting discount amount.
func CalculateDiscount(script string, order map[string]interface{}) (float64, error) {
	vm := goja.New()

	// Inject all order fields as global variables in the VM
	for key, value := range order {
		vm.Set(key, value)
	}

	// Execute the script
	result, err := vm.RunString(script)
	if err != nil {
		return 0.0, fmt.Errorf("script execution error: %w", err)
	}

	// Convert result to float64
	discount, ok := result.Export().(float64)
	if !ok {
		// Try to convert from other numeric types (e.g., int, float32)
		switch v := result.Export().(type) {
		case int:
			discount = float64(v)
		case int32:
			discount = float64(v)
		case int64:
			discount = float64(v)
		case float32:
			discount = float64(v)
		default:
			return 0.0, fmt.Errorf("script must return a number, got %T", result.Export())
		}
	}

	return discount, nil
}

```

## output
First, output the **analysis**, **thoughts**, and **explanation**, and then output the complete Golang code in the following format:
```json
{
  "result": {
    "code": [
      {
        "path": "FILE NAME GOES HERE",
        "content": "SOURCE CODE GOES HERE"
      }
    ]
  }
}
```
