// Copyright (c) 2025 Alibaba Group and its affiliates

// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at

//     http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.example.service.UrlPreviewService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.UUID;

/**
 * Security tests for the UrlPreviewService.
 * This class attempts to exploit potential SSRF vulnerabilities.
 * The tests are designed to FAIL if the vulnerability exists and PASS if it has been fixed.
 */
public class SecurityTest {

    private HttpServer localServer;
    private int localServerPort;
    private final String ATTACK_ENDPOINT_PATH = "/ssrf-test-endpoint";
    // 新增：用于跟踪HTTP请求是否被接收的标志
    private volatile boolean httpRequestReceived;

    private final String SECRET_FILE_MARKER = "SECRET_FILE_CONTENT_" + UUID.randomUUID().toString();

    /**
     * Rule to create a temporary folder for test files.
     * This folder and its contents are automatically deleted after each test.
     */
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    /**
     * Sets up a local lightweight HTTP server before each test.
     * This server acts as the "internal service" that an attacker would target.
     */
    @Before
    public void setUp() throws IOException {
        httpRequestReceived = false;

        int maxRetries = 10;
        for (int i = 0; i < maxRetries; i++) {
            try {
                try (ServerSocket s = new ServerSocket(0)) {
                    localServerPort = s.getLocalPort();
                }

                localServer = HttpServer.create(new InetSocketAddress("0.0.0.0", localServerPort), 0);
                localServer.createContext(ATTACK_ENDPOINT_PATH, new LocalServerHandler());
                localServer.setExecutor(null);
                localServer.start();

                System.out.println("Local test server started successfully on port: " + localServerPort);
                return;

            } catch (IOException e) {
                System.err.println("Port " + localServerPort + " was occupied, retrying... (" + (i + 1) + "/" + maxRetries + ")");
                if (i == maxRetries - 1) {
                    throw new IOException("Failed to start local test server after " + maxRetries + " attempts.", e);
                }
            }
        }
    }

    /**
     * Stops the local HTTP server after each test to release resources.
     */
    @After
    public void tearDown() {
        if (localServer != null) {
            localServer.stop(0);
        }
    }

    /**
     * 测试通过提供一个指向本地资源的直接 IP 地址来模拟 SSRF 攻击。
     */
    @Test
    public void testSsrAttackWithDirectIpUrl() {
        String maliciousUrl = String.format("http://127.0.0.1:%d%s", localServerPort, ATTACK_ENDPOINT_PATH);
        assertSsrfaIsBlocked(maliciousUrl, "Direct IP access");
    }

    /**
     * 测试通过提供一个使用 "file://" 协议的 URL 来模拟 SSRF/LFI（本地文件包含）攻击。
     */
    @Test
    public void testSsrAttackWithLocalFileUrl() throws IOException {
        File tempFile = tempFolder.newFile("secret.txt");
        try (PrintWriter out = new PrintWriter(new FileWriter(tempFile))) {
            out.println(SECRET_FILE_MARKER);
        }

        String maliciousFileUrl = tempFile.toURI().toString();
        UrlPreviewService urlPreviewService = new UrlPreviewService();
        String result = urlPreviewService.getUrlContentPreview(maliciousFileUrl);

        Assert.assertFalse(
                "LFI Attack Successful! The service read a local file from the filesystem.",
                result != null && result.contains(SECRET_FILE_MARKER)
        );
    }

    /**
     * 测试通过提供一个使用 "file://" 协议的 URL 来模拟 SSRF/LFI（本地文件包含）攻击，并添加url绕过。
     */
    @Test
    public void testSsrAttackWithLocalFileUrlAndBypass() throws IOException {
        File tempFile = tempFolder.newFile("secret.txt");
        try (PrintWriter out = new PrintWriter(new FileWriter(tempFile))) {
            out.println(SECRET_FILE_MARKER);
        }

        String maliciousFileUrl = tempFile.toURI().toString();
        maliciousFileUrl = "url:" + maliciousFileUrl;
        UrlPreviewService urlPreviewService = new UrlPreviewService();
        String result = urlPreviewService.getUrlContentPreview(maliciousFileUrl);

        Assert.assertFalse(
                "LFI Attack Successful! The service read a local file from the filesystem via: " + maliciousFileUrl,
                result != null && result.contains(SECRET_FILE_MARKER)
        );
    }

    /**
     * 测试通过 DNS 解析服务绕过基于主机名的 SSRF 过滤器。
     */
    @Test
    public void testSsrAttackViaDns() {
        String maliciousUrl = String.format("http://127.0.0.1.nip.io:%d%s", localServerPort, ATTACK_ENDPOINT_PATH);
        assertSsrfaIsBlocked(maliciousUrl, "Magic DNS service (nip.io)");
    }

    /**
     * 测试通过 HTTP 302 重定向到内部网络的 SSRF 攻击。
     */
    @Test
    public void testRedirectToInternalNetworkAccess() throws IOException {
        String url = String.format("http://localhost:%d%s", localServerPort, ATTACK_ENDPOINT_PATH);
        String redirectUrlFormat = "http://drsupport.oss-cn-beijing.aliyuncs.com/heimdall_ssrf_redirect.png?" + url;
        assertSsrfaIsBlocked(redirectUrlFormat, "302 redirect to internal network");
    }

    /**
     * 测试使用各种形式的 localhost 和环回地址（如 'localhost', '0'）的 SSRF 攻击。
     */
    @Test
    public void testSsrAttackWithLoopbackVariations() {
        assertSsrfaIsBlocked(String.format("http://localhost:%d%s", localServerPort, ATTACK_ENDPOINT_PATH),
                "Hostname 'localhost'");

        assertSsrfaIsBlocked(String.format("http://0:%d%s", localServerPort, ATTACK_ENDPOINT_PATH),
                "Special address '0'");

        assertSsrfaIsBlocked(String.format("http://0.0.0.0:%d%s", localServerPort, ATTACK_ENDPOINT_PATH),
                "Special address '0.0.0.0'");
    }

    /**
     * 测试使用 IP 地址编码（十进制、八进制、十六进制）进行混淆的 SSRF 攻击。
     */
    @Test
    public void testSsrAttackWithEncodedIpAddresses() {
        assertSsrfaIsBlocked(String.format("http://2130706433:%d%s", localServerPort, ATTACK_ENDPOINT_PATH),
                "Decimal-encoded IP");

        assertSsrfaIsBlocked(String.format("http://0177.0.0.1:%d%s", localServerPort, ATTACK_ENDPOINT_PATH),
                "Octal-encoded IP");

        assertSsrfaIsBlocked(String.format("http://0x7f.0.0.1:%d%s", localServerPort, ATTACK_ENDPOINT_PATH),
                "Hex-encoded IP");
    }

    /**
     * 测试使用 IPv6 环回地址（包括标准和 IPv4 映射格式）的 SSRF 攻击。
     */
    @Test
    public void testSsrAttackWithIPv6LoopbackAddresses() {
        assertSsrfaIsBlocked(String.format("http://[::1]:%d%s", localServerPort, ATTACK_ENDPOINT_PATH),
                "IPv6 loopback '[::1]'");

        assertSsrfaIsBlocked(String.format("http://[::ffff:127.0.0.1]:%d%s", localServerPort, ATTACK_ENDPOINT_PATH),
                "IPv4-mapped IPv6 loopback");

        assertSsrfaIsBlocked(String.format("http://[::ffff:7f00:1]:%d%s", localServerPort, ATTACK_ENDPOINT_PATH),
                "IPv4-mapped IPv6 loopback (Hex)");
    }

    /**
     * 测试利用 URL 认证语法（'safedomain@127.0.0.1'）混淆主机的 SSRF 攻击。
     */
    @Test
    public void testSsrAttackWithUrlAuthenticationSyntax() {
        String maliciousUrl = String.format("http://safedomain.com@127.0.0.1:%d%s", localServerPort, ATTACK_ENDPOINT_PATH);
        assertSsrfaIsBlocked(maliciousUrl, "URL authentication syntax with '@'");
    }

    /**
     * 测试利用同形异义字符（视觉上相似的 Unicode 字符）混淆 IP 地址的 SSRF 攻击。
     */
    @Test
    public void testSsrAttackWithHomoglyphObfuscation() {
        String urlWithCircledDigit = String.format("http://1②7.0.0.1:%d%s", localServerPort, ATTACK_ENDPOINT_PATH);
        assertSsrfaIsBlocked(urlWithCircledDigit, "Unicode circled digit bypass (1②7.0.0.1)");

        String urlWithFullWidthDot = String.format("http://127.0.0。1:%d%s", localServerPort, ATTACK_ENDPOINT_PATH);
        assertSsrfaIsBlocked(urlWithFullWidthDot, "Full-width dot bypass (127.0.0。1)");
    }

    /**
     * 它现在检查本地服务器是否收到了HTTP请求。
     */
    private void assertSsrfaIsBlocked(String maliciousUrl, String attackDescription) {
        // 在每次断言前，确保标志是false
        this.httpRequestReceived = false;

        UrlPreviewService urlPreviewService = new UrlPreviewService();
        // 调用方法，我们不再关心其返回值
        try {
            urlPreviewService.getUrlContentPreview(maliciousUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 核心修改：断言我们的本地服务器没有收到请求
        Assert.assertFalse(
                "SSRF Attack Successful! (" + attackDescription + "). The service made a forbidden HTTP request to: " + maliciousUrl,
                this.httpRequestReceived
        );
    }


    /**
     * 修改后的Handler。
     * 它现在的主要作用是设置 httpRequestReceived 标志为 true。
     * 响应内容已不再重要，但为了保持HTTP交互的完整性，仍然发送一个简单的响应。
     */
    private class LocalServerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 关键修改：一旦接收到请求，就设置标志位
            SecurityTest.this.httpRequestReceived = true;

            System.out.println("Local test server received a request on path: " + exchange.getRequestURI().getPath());

            // 响应内容已不重要，但最好还是完成HTTP交换
            String response = "Request Received";
            byte[] responseBytes = response.getBytes();
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
}
