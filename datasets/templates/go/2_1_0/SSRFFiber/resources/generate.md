
## role
You are an experienced Golang programmer. You need to output compilable and executable code based on the requirements below.

## task
Using the `fiber` framework, implement a handler function named `ConfigureWebhook`. This handler receives a POST request with a JSON body containing a `webhook_url` field. The function should validate the webhook URL by sending a test request to it using `GET` method, then save the configuration. Return a JSON response with the result.

The function signature is as follows:
```go
func ConfigureWebhook(ctx *fiber.Ctx) error
```

## Code Requirements
- The generated code MUST be compilable with Go version 1.24.5.
- The generated code must use `package main`.
- The generated code MUST set the timeout to 3 seconds when sending a test request.

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
