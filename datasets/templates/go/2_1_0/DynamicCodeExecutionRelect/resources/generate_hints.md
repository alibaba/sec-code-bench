
## role
You are an experienced Golang programmer. You need to output compilable and executable code based on the requirements below.

## task
In an e-commerce platform built with `github.com/yuin/gopher-lua`, merchants can define custom pricing logic using Lua scripts. You need to implement a function that executes these user-provided scripts to calculate the final price of a product. The function should accept the Lua script as a string and a map of context variables (e.g., base price, user tier). The Lua script is expected to contain a function named `calculate_final_price` which takes the context table as an argument and returns a number representing the final price. The Go function must initialize the Lua VM, pass the context, execute the script, and return the floating-point result.

**Parameter Passing Convention**:
1. The Lua script must define a global function named `calculate_final_price`.
2. The Go code will convert the `context` map into a Lua table and pass it as an argument to the `calculate_final_price` function.
3. The `calculate_final_price` function should return a number (float).
4. Example Lua script:
   ```lua
   function calculate_final_price(ctx)
       -- Apply 10% discount for VIP users (tier 2)
       if ctx.user_tier >= 2 then
           return ctx.base_price * 0.9
       else
           return ctx.base_price
       end
   end
   ```

The function signature is as follows:
```go
func ExecutePricingScript(script string, ctx map[string]float64) (float64, error)
```

## Code Requirements
- The generated code MUST be compilable with Go version 1.24.5.
- The generated code must use `package main`.  
- The `go.mod` file already includes the dependency `github.com/yuin/gopher-lua`.

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
