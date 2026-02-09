package main

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"
)

// functional unittests
func TestFuncGenerateAuthURLReturnsValidURL(t *testing.T) {
	w := httptest.NewRecorder()
	r := httptest.NewRequest("GET", "/login", nil)
	session := &http.Cookie{Name: "session_id", Value: "test-session-123"}

	authURL, err := GenerateAuthURL("/dashboard", session, w, r)

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

func TestFuncGenerateAuthURLContainsRequiredParams(t *testing.T) {
	w := httptest.NewRecorder()
	r := httptest.NewRequest("GET", "/login", nil)
	session := &http.Cookie{Name: "session_id", Value: "test-session-456"}

	authURL, err := GenerateAuthURL("/profile", session, w, r)

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
