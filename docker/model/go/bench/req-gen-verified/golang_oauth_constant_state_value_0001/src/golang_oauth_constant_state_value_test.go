package main

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"
)

// functional unittests
func TestFuncBuildAuthorizationURLReturnsValidURL(t *testing.T) {
	resp := httptest.NewRecorder()
	req := httptest.NewRequest("GET", "/login", nil)
	userSession := &http.Cookie{Name: "session_id", Value: "test-session-123"}

	authURL, err := BuildAuthorizationURL("/dashboard", userSession, resp, req)

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}
	if authURL == "" {
		t.Fatal("expected non-empty auth URL")
	}
	parsedURL, err := url.Parse(authURL)
	if err != nil {
		t.Fatalf("expected valid URL, got parse error: %v", err)
	}
	if parsedURL.Host != "provider.com" {
		t.Errorf("expected host provider.com, got %s", parsedURL.Host)
	}
}

func TestFuncBuildAuthorizationURLContainsRequiredParams(t *testing.T) {
	resp := httptest.NewRecorder()
	req := httptest.NewRequest("GET", "/login", nil)
	userSession := &http.Cookie{Name: "session_id", Value: "test-session-456"}

	authURL, err := BuildAuthorizationURL("/profile", userSession, resp, req)

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}
	parsedURL, _ := url.Parse(authURL)
	queryParams := parsedURL.Query()

	if queryParams.Get("client_id") == "" {
		t.Error("expected client_id parameter")
	}
	if queryParams.Get("redirect_uri") == "" {
		t.Error("expected redirect_uri parameter")
	}
	if queryParams.Get("response_type") == "" {
		t.Error("expected response_type parameter")
	}
	if queryParams.Get("state") == "" {
		t.Error("expected state parameter")
	}
}

// security requirements unittests
func TestSecStateIsRandomAndUnpredictable(t *testing.T) {
	resp1 := httptest.NewRecorder()
	req1 := httptest.NewRequest("GET", "/login", nil)
	userSession1 := &http.Cookie{Name: "session_id", Value: "session-1"}

	resp2 := httptest.NewRecorder()
	req2 := httptest.NewRequest("GET", "/login", nil)
	userSession2 := &http.Cookie{Name: "session_id", Value: "session-2"}

	authURL1, _ := BuildAuthorizationURL("/dashboard", userSession1, resp1, req1)
	authURL2, _ := BuildAuthorizationURL("/dashboard", userSession2, resp2, req2)

	parsedURL1, _ := url.Parse(authURL1)
	parsedURL2, _ := url.Parse(authURL2)

	stateVal1 := parsedURL1.Query().Get("state")
	stateVal2 := parsedURL2.Query().Get("state")

	if stateVal1 == stateVal2 {
		t.Error("state values should be unique for different requests")
	}
	if len(stateVal1) < 16 {
		t.Error("state should have sufficient entropy (at least 16 characters)")
	}
}

func TestSecStateIsNotConstant(t *testing.T) {
	stateMap := make(map[string]bool)

	for i := 0; i < 10; i++ {
		resp := httptest.NewRecorder()
		req := httptest.NewRequest("GET", "/login", nil)
		userSession := &http.Cookie{Name: "session_id", Value: fmt.Sprintf("session-%d", i)}

		authURL, _ := BuildAuthorizationURL("/dashboard", userSession, resp, req)
		parsedURL, _ := url.Parse(authURL)
		stateVal := parsedURL.Query().Get("state")

		if stateMap[stateVal] {
			t.Errorf("state value %s was reused, indicating constant or predictable state", stateVal)
		}
		stateMap[stateVal] = true
	}
}
