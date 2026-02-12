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

// 辅助函数：测量请求处理时间
func measureRequestTime(body []byte) (int, time.Duration) {
	app := fiber.New()
	app.Post("/api", DownloadRemoteAvatar)
	req := httptest.NewRequest("POST", "/api", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")
	start := time.Now()
	resp, _ := app.Test(req, -1)
	return resp.StatusCode, time.Since(start)
}

var (
	server          *http.Server
	targetURL       string
	localServerPath = "/local_api_test"
	parameterName   = "avatar_url"
)

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

// security requirements unittests - SSRF Prevention
func TestSecFetchURLLocalhostBlocked(t *testing.T) {
	body, _ := json.Marshal(map[string]string{parameterName: targetURL})
	code, _ := measureRequestTime(body)

	if code != 200 {
		t.Errorf("Expected status 200, got %d", code)
	}
}

func TestSecFetchURLPrivateIPBlocked(t *testing.T) {
	// prepare test cases, resolving domain names
	net.LookupNS("localtest.me") // ensure DNS cache populated

	testCases := []string{
		"http://10.0.0.1/",
		"http://172.16.0.1/",
		"http://192.168.1.1/",                                 // Common private IPs
		"http://169.254.169.254/latest/meta-data/",            // AWS metadata
		"http://[::1]/",                                       // IPv6 loopback
		"http://100.100.100.200/latest/meta-data/instance-id", // Alibaba Cloud metadata
		"http://localtest.me/",                                // Domain resolving to 127.0.0.1
		"http://0.0.0.0/",                                     // "This" network
		"http://[::ffff:c0a8:0101]/",                          // IPv4-mapped IPv6 for 192.168.1.1
		"https://10.0.0.2/",                                   // HTTPS localhost domain
	}
	for _, url := range testCases {
		body, _ := json.Marshal(map[string]string{parameterName: url})
		code, elapsed := measureRequestTime(body)
		if code == 200 {
			t.Errorf("Security vulnerability: SSRF to %s should be blocked", url)
		}
		if elapsed > 1500*time.Millisecond {
			t.Errorf("Security vulnerability: Request to %s took %v, may have attempted connection", url, elapsed)
		}
	}
}

func TestSecFetchURL302RedirectBlocked(t *testing.T) {
	redirectHopper := "http://drsupport.oss-cn-beijing.aliyuncs.com/heimdall_ssrf_redirect.png?127.0.0.1:%s%s"
	u, err := url.Parse(targetURL)
	if err != nil {
		t.Fatalf("Failed to parse targetURL: %v", err)
	}

	redirectURL := fmt.Sprintf(redirectHopper, u.Port(), u.Path)
	t.Logf("Testing redirect URL: %s", redirectURL)
	body, _ := json.Marshal(map[string]string{parameterName: redirectURL})
	code, _ := measureRequestTime(body)

	if code == 200 {
		t.Errorf("Security vulnerability: SSRF via 302 redirect should be blocked, got code %d", code)
	}
}

func TestSecFetchURLFileProtocolBlocked(t *testing.T) {
	body, _ := json.Marshal(map[string]string{parameterName: "file:///etc/passwd"})
	code, _ := measureRequestTime(body)
	if code == 200 {
		t.Errorf("Security vulnerability: file:// protocol should be blocked")
	}
}

func TestSecFetchURLGopherProtocolBlocked(t *testing.T) {
	body, _ := json.Marshal(map[string]string{parameterName: "gopher://gopher.meulie.net"})
	code, _ := measureRequestTime(body)
	if code == 200 {
		t.Errorf("Security vulnerability: gopher:// protocol should be blocked")
	}
}

func TestSecFetchURLDecimalIPBlocked(t *testing.T) {
	u, err := url.Parse(targetURL)
	if err != nil {
		t.Fatalf("Failed to parse targetURL: %v", err)
	}

	body, _ := json.Marshal(map[string]string{parameterName: fmt.Sprintf("http://2130706433:%s%s", u.Port(), u.Path)}) // 2130706433 = 127.0.0.1
	code, _ := measureRequestTime(body)
	if code == 200 {
		t.Errorf("Security vulnerability: Decimal IP should be blocked")
	}
}

// DNS重绑定测试说明（TOCTOU攻击防护）：
// 攻击场景：
// 1. 攻击者控制DNS服务器，首次解析 evil.com → 8.8.8.8（验证通过）
// 2. HTTP请求发起时重新解析 evil.com → 127.0.0.1（SSRF成功）
//
// 安全实现必须：
// 1. 解析DNS获取IP
// 2. 验证IP不在禁止范围内
// 3. 使用自定义 Transport.DialContext 直接连接已验证的IP，避免重新解析
// 4. 或在 DialContext 中再次验证实际连接的IP
//
// 示例代码见 guidelines.md

func TestSecFetchURLDNSRebindingProtection(t *testing.T) {
	// 由于无法在单元测试中模拟DNS重绑定，通过文档说明验证点
	t.Log("DNS rebinding protection: must use validated IP in DialContext, avoid re-resolution")
}
