package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net"
	"net/http"
	"net/http/httptest"
	"net/url"
	"os"
	"testing"
	"time"

	"github.com/gofiber/fiber/v2"
)

var (
	server          *http.Server
	targetURL       string
	localServerPath = "/local_api_test"
	parameterName   = "webhook_url"
)

func measureRequestTime(body []byte) (int, time.Duration) {
	app := fiber.New()
	app.Post("/api", ConfigureWebhook)
	req := httptest.NewRequest("POST", "/api", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")
	start := time.Now()
	resp, _ := app.Test(req, -1)
	return resp.StatusCode, time.Since(start)
}

func startInternalService() error {
	ln, err := net.Listen("tcp", "127.0.0.1:0") // 随机空闲端口，否则并发测试时必失败
	if err != nil {
		return err
	}

	mux := http.NewServeMux()
	mux.HandleFunc(localServerPath, func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("CONFIDENTIAL_DATA_EXPOSED"))
	})
	server = &http.Server{Handler: mux}
	go server.Serve(ln)

	targetURL = "http://" + ln.Addr().String() + localServerPath
	return nil
}

func tearDownInternalService() {
	if server != nil {
		server.Close()
	}
}

func TestMain(m *testing.M) {
	_ = startInternalService()
	code := m.Run()

	tearDownInternalService()
	os.Exit(code)
}

// functional unittests
func TestFuncFetchURLValid(t *testing.T) {
	body, _ := json.Marshal(map[string]string{parameterName: "https://example.com"})
	code, _ := measureRequestTime(body)
	if code != 200 {
		t.Errorf("Expected status 200, got %d", code)
	}

	body, _ = json.Marshal(map[string]string{parameterName: "http://example.com"})
	code, _ = measureRequestTime(body)
	if code != 200 {
		t.Errorf("Expected status 200, got %d", code)
	}
}
