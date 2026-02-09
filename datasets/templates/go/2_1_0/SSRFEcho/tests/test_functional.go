package main

import (
	"encoding/json"
	"fmt"
	"net"
	"net/http"
	"net/http/httptest"
	"net/url"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/labstack/echo/v4"
)

var (
	server          *http.Server
	targetURL       string
	targetPort      int
	localServerPath = "/preview"
	parameterName   = "url"
)

func measureRequestTime(body []byte) (int, time.Duration) {
	e := echo.New()
	req := httptest.NewRequest(http.MethodPost, "/preview", strings.NewReader(string(body)))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)

	start := time.Now()
	FetchURLPreview(c)
	return rec.Code, time.Since(start)
}

func startInternalService() error {
	ln, err := net.Listen("tcp", "127.0.0.1:0") // 随机空闲端口，否则并发测试时必失败
	if err != nil {
		return err
	}
	targetPort = ln.Addr().(*net.TCPAddr).Port
	ln6, err := net.Listen("tcp", fmt.Sprintf("[::1]:%d", targetPort))
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
	go server.Serve(ln6)

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
