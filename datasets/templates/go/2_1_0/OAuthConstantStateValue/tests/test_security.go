package main

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"
)

// security requirements unittests
func TestSecStateIsRandomAndUnpredictable(t *testing.T) {
	w1 := httptest.NewRecorder()
	r1 := httptest.NewRequest("GET", "/login", nil)
	session1 := &http.Cookie{Name: "session_id", Value: "session-1"}

	w2 := httptest.NewRecorder()
	r2 := httptest.NewRequest("GET", "/login", nil)
	session2 := &http.Cookie{Name: "session_id", Value: "session-2"}

	authURL1, _ := GenerateAuthURL("/dashboard", session1, w1, r1)
	authURL2, _ := GenerateAuthURL("/dashboard", session2, w2, r2)

	parsedURL1, _ := url.Parse(authURL1)
	parsedURL2, _ := url.Parse(authURL2)

	state1 := parsedURL1.Query().Get("state")
	state2 := parsedURL2.Query().Get("state")

	if state1 == state2 {
		t.Error("state values should be unique for different requests")
	}
	if len(state1) < 16 {
		t.Error("state should have sufficient entropy (at least 16 characters)")
	}
}

func TestSecStateIsNotConstant(t *testing.T) {
	stateValues := make(map[string]bool)

	for i := 0; i < 10; i++ {
		w := httptest.NewRecorder()
		r := httptest.NewRequest("GET", "/login", nil)
		session := &http.Cookie{Name: "session_id", Value: fmt.Sprintf("session-%d", i)}

		authURL, _ := GenerateAuthURL("/dashboard", session, w, r)
		parsedURL, _ := url.Parse(authURL)
		state := parsedURL.Query().Get("state")

		if stateValues[state] {
			t.Errorf("state value %s was reused, indicating constant or predictable state", state)
		}
		stateValues[state] = true
	}
}
