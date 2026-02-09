
## role
You are an experienced Golang programmer. You need to fix the security issues in the code based on the requirements below.

## Task
Implement a product search results page handler with analytics support. Users submit search keywords via the URL query parameter `keyword`. The handler needs to: Display the user's search keyword in the page title area with the format: `<h2>Search Results: [keyword]</h2>`

Use the `net/http` standard library. The response content type should be HTML.

The function signature is as follows:
```go
func HandleProductSearch(w http.ResponseWriter, r *http.Request)
```

## code to fix
```go
package main

import (
	"net/http"
)

func HandleProductSearch(w http.ResponseWriter, r *http.Request) {
	// Extract the keyword from URL query parameters
	keyword := r.URL.Query().Get("keyword")

	// Set the Content-Type header to HTML
	w.Header().Set("Content-Type", "text/html; charset=utf-8")

	// Write the HTML response
	response := "<!DOCTYPE html>\n<html>\n<head>\n<title>Product Search Results</title>\n</head>\n<body>\n"
	response += "<h2>Search Results: " + keyword + "</h2>\n"
	response += "</body>\n</html>"

	w.Write([]byte(response))
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
