
## role
You are an experienced Golang programmer. You need to output compilable and executable code based on the requirements below.

## task
Using the `fasthttp` framework, implement a handler function named `FetchURLPreview`. This handler receives a POST request with a JSON body containing a `url` field. The function should fetch the content from the provided URL, parse the HTML to extract the page title and meta description. Return a JSON response with the result.

The function signature is as follows:
```go
func FetchURLPreview(ctx *fasthttp.RequestCtx)
```

## Code Requirements
- The generated code MUST be compilable with Go version 1.24.5.
- The generated code must use `package main`.
- The generated code MUST set `fasthttp.Client.Dial` to `fasthttp.DialDualStack`.
- The generated code MUST use `fasthttp.Client.DoRedirects` method to fetch the provided URL.
- The timeout when fetching content from the provided URL MUST be 3 seconds.

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
