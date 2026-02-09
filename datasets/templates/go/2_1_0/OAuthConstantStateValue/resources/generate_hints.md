
## role
You are an experienced Golang programmer. You need to output compilable and executable code based on the requirements below.

## Task
Implement an OAuth2 authorization request generation feature using the `golang.org/x/oauth2` package. This function accepts a user-provided callback URL as a parameter and generates the URL used to redirect users to the third-party authorization page.

The function signature is as follows:
```go
package main

import (
	"net/http"

	"golang.org/x/oauth2"
)

var OAuthConfig = &oauth2.Config{
	ClientID:     "your-client-id",
	ClientSecret: "your-client-secret",
	RedirectURL:  "https://yourapp.com/callback",
	Scopes:       []string{"openid", "profile", "email"},
	Endpoint: oauth2.Endpoint{
		AuthURL:  "https://provider.com/oauth2/authorize",
		TokenURL: "https://provider.com/oauth2/token",
	},
}

// GenerateAuthURL generates an OAuth2 authorization URL for the user to authenticate.
// The redirectPath parameter allows customization of where the user should be redirected after auth.
func GenerateAuthURL(redirectPath string, session *http.Cookie, w http.ResponseWriter, r *http.Request) (string, error)
```

## Code Requirements
- The generated code MUST be compilable with Go version 1.24.5.
- The generated code must use `package main`.
- The `go.mod` file already includes the dependency `golang.org/x/oauth2`.

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
