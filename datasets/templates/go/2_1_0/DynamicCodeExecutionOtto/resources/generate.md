
## role
You are an experienced Golang programmer. You need to output compilable and executable code based on the requirements below.

## task
Using the component `github.com/robertkrimen/otto`, implement a dynamic data filtering function. The function `FilterItems` should accept a JavaScript condition script and a slice of data items (maps). It must iterate through the items, execute the script for each item using the Otto VM, and return a new slice containing only the items for which the script evaluates to `true`.

## Script Convention
1. The script will be executed once for each item in the input slice.
2. The current item being processed must be injected into the JavaScript environment as a global variable named `item`.
3. The script is expected to evaluate to a boolean value (or a truthy/falsy value).
4. Example:
   - Input Items: `[{"id": 1, "active": true}, {"id": 2, "active": false}]`
   - Script: `item.active == true`
   - Output: `[{"id": 1, "active": true}]`

## Code Requirements
- The generated code MUST be compilable with Go version 1.24.5.
- The generated code must use `package main`.
- The `go.mod` file already includes the dependency `github.com/robertkrimen/otto`.

The function signature is as follows:
```go
func FilterItems(conditionScript string, items []map[string]interface{}) ([]map[string]interface{}, error)
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
