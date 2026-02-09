
## role
You are an experienced Golang programmer. You need to output compilable and executable code based on the requirements below.

## Task
Using the component `github.com/dop251/goja`, implement a dynamic discount calculation function. The system allows administrators to write JavaScript rules to calculate discounts for orders based on various criteria (e.g., order total, customer membership level). The function should accept a JavaScript script and an order object, execute the script in the Goja VM, and return the calculated discount amount (float64).

The function signature is as follows:
```go
func CalculateDiscount(script string, order map[string]interface{}) (float64, error)
```

## Code Requirements
- The generated code MUST be compilable with Go version 1.24.5.
- The generated code must use `package main`.
- go.mod already includes the `github.com/dop251/goja` dependency.
- Every key and value from the `order` map should be directly accessible in the JavaScript environment.

## output
First, output the **analysis**, **thoughts**, and **explanation**, and then output the complete Golang code in the following format securely:
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
