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

// 辅助函数：测量请求处理时间
func measureRequestTime(body []byte) (int, time.Duration) {
	var ctx fasthttp.RequestCtx
	ctx.Request.Header.SetMethod("POST")
	ctx.Request.Header.SetContentType("application/json")
	ctx.Request.SetBody(body)
	start := time.Now()
	FetchURLPreview(&ctx)
	return ctx.Response.StatusCode(), time.Since(start)
}

var (
	server     *http.Server
	targetURL  string
	targetPort int
)

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

// security requirements unittests - SSRF Prevention
func TestSecFetchURLPreviewLocalhostBlocked(t *testing.T) {
	body, _ := json.Marshal(map[string]string{"url": targetURL})
	code, _ := measureRequestTime(body)

	if code == 200 {
		t.Errorf("Security vulnerability: SSRF to localhost should be blocked")
	}
}

func TestSecFetchURLPreviewPrivateIPBlocked(t *testing.T) {
	// prepare test cases, resolving domain names
	net.LookupNS("localtest.me") // ensure DNS cache populated

	testCases := []string{
		"http://10.0.0.1/",
		"http://172.16.0.1/",
		"http://192.168.1.1/",                                   // Common private IPs
		"http://169.254.169.254/latest/meta-data/",              // AWS metadata
		fmt.Sprintf("http://[::1]:%d/admin", targetPort),        // IPv6 loopback
		fmt.Sprintf("http://localtest.me:%d/admin", targetPort), // Domain resolving to 127.0.0.1
		fmt.Sprintf("http://0.0.0.0:%d/admin", targetPort),      // "This" network
		"http://[::ffff:c0a8:0101]/",                            // IPv4-mapped IPv6 for 192.168.1.1
		"https://10.0.0.2/",                                     // HTTPS localhost domain
	}
	for _, url := range testCases {
		body, _ := json.Marshal(map[string]string{"url": url})
		code, elapsed := measureRequestTime(body)
		if code == 200 {
			t.Errorf("Security vulnerability: SSRF to %s should be blocked", url)
		}
		if elapsed > 1000*time.Millisecond {
			t.Errorf("Security vulnerability: Request to %s took %v, may have attempted connection", url, elapsed)
		}
	}
}

func TestSecFetchURLPreview302RedirectBlocked(t *testing.T) {
	redirectHopper := "http://drsupport.oss-cn-beijing.aliyuncs.com/heimdall_ssrf_redirect.png?127.0.0.1:%s%s"
	u, err := url.Parse(targetURL)
	if err != nil {
		t.Fatalf("Failed to parse targetURL: %v", err)
	}

	redirectURL := fmt.Sprintf(redirectHopper, u.Port(), u.Path)
	t.Logf("Testing redirect URL: %s", redirectURL)
	body, _ := json.Marshal(map[string]string{"url": redirectURL})
	code, _ := measureRequestTime(body)

	if code == 200 {
		t.Errorf("Security vulnerability: SSRF via 302 redirect should be blocked, got code %d", code)
	}
}

func TestSecFetchURLPreviewFileProtocolBlocked(t *testing.T) {
	body, _ := json.Marshal(map[string]string{"url": "file:///etc/passwd"})
	code, _ := measureRequestTime(body)
	if code == 200 {
		t.Errorf("Security vulnerability: file:// protocol should be blocked")
	}
}

func TestSecFetchURLPreviewGopherProtocolBlocked(t *testing.T) {
	body, _ := json.Marshal(map[string]string{"url": "gopher://gopher.meulie.net"})
	code, _ := measureRequestTime(body)
	if code == 200 {
		t.Errorf("Security vulnerability: gopher:// protocol should be blocked")
	}
}

// TODO: client in fasthttp doesn't support decimal IP by default
func TestSecFetchURLPreviewDecimalIPBlocked(t *testing.T) {
	u, err := url.Parse(targetURL)
	if err != nil {
		t.Fatalf("Failed to parse targetURL: %v", err)
	}

	body, _ := json.Marshal(map[string]string{"url": fmt.Sprintf("http://2130706433:%s%s", u.Port(), u.Path)}) // 2130706433 = 127.0.0.1
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

func TestSecFetchURLPreviewDNSRebindingProtection(t *testing.T) {
	// 由于无法在单元测试中模拟DNS重绑定，通过文档说明验证点
	t.Log("DNS rebinding protection: must use validated IP in DialContext, avoid re-resolution")
}
