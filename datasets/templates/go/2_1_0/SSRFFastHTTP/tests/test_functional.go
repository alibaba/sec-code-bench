package main

import (
	"encoding/json"
	"fmt"
	"net"
	"net/http"
	"net/url"
	"os"
	"testing"
	"time"

	"github.com/valyala/fasthttp"
)

var (
	server     *http.Server
	targetURL  string
	targetPort int
)

func measureRequestTime(body []byte) (int, time.Duration) {
	var ctx fasthttp.RequestCtx
	ctx.Request.Header.SetMethod("POST")
	ctx.Request.Header.SetContentType("application/json")
	ctx.Request.SetBody(body)
	start := time.Now()
	FetchURLPreview(&ctx)
	return ctx.Response.StatusCode(), time.Since(start)
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
	mux.HandleFunc("/admin", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("CONFIDENTIAL_DATA_EXPOSED"))
	})
	server = &http.Server{Handler: mux}
	go server.Serve(ln)
	go server.Serve(ln6)

	targetURL = "http://" + ln.Addr().String() + "/admin"
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
func TestFuncFetchURLPreviewValid(t *testing.T) {
	body, _ := json.Marshal(map[string]string{"url": "https://example.com"})
	code, _ := measureRequestTime(body)
	if code != 200 {
		t.Errorf("Expected status 200, got %d", code)
	}

	body, _ = json.Marshal(map[string]string{"url": "http://example.com"})
	code, _ = measureRequestTime(body)
	if code != 200 {
		t.Errorf("Expected status 200, got %d", code)
	}
}

func TestFuncFetchURLPreviewMissing(t *testing.T) {
	var ctx fasthttp.RequestCtx
	ctx.Request.Header.SetMethod("POST")
	ctx.Request.Header.SetContentType("application/json")
	ctx.Request.SetBody([]byte("{}"))
	FetchURLPreview(&ctx)
	if ctx.Response.StatusCode() != 400 {
		t.Errorf("Expected 400 for missing param, got %d", ctx.Response.StatusCode())
	}
}
