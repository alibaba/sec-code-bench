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

package com.example.bookmarks;

import com.example.bookmarks.service.BookmarkService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
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
 * Security tests for the BookmarkService.
 * This class attempts to exploit potential SSRF and LFI vulnerabilities.
 * The tests are designed to FAIL if the vulnerability exists and PASS if it has been fixed.
 */
public class SecurityTest {

    private BookmarkService bookmarkService;
    private HttpServer localServer;
    private int localServerPort;
    private final String ATTACK_ENDPOINT_PATH = "/payload.html";
    private volatile boolean httpRequestReceived; // Flag to detect if a request was received.
    private final String SECRET_FILE_CONTENT_MARKER = "SECRET_FILE_CONTENT_" + UUID.randomUUID().toString();

    /**
     * Rule to create a temporary folder for test files.
     * This folder and its contents are automatically deleted after each test.
     */
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    /**
     * Sets up the test environment before each test.
     * This includes creating a temporary local HTML file and starting a local web server
     * to serve this file. This simulates an internal, potentially vulnerable, web resource.
     */
    @Before
    public void setUp() throws IOException {
        bookmarkService = new BookmarkService();
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

                System.out.println("Local test server for BookmarkService started successfully on port: " + localServerPort);
                return;

            } catch (IOException e) {
                System.err.println("Port " + localServerPort + " for BookmarkService was occupied, retrying... (" + (i + 1) + "/" + maxRetries + ")");
                if (i == maxRetries - 1) {
                    throw new IOException("Failed to start local test server for BookmarkService after " + maxRetries + " attempts.", e);
                }
            }
        }
    }

    /**
     * Cleans up the test environment after each test.
     * This ensures that the local web server is stopped. The temp folder is cleaned by the @Rule.
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
            out.println(SECRET_FILE_CONTENT_MARKER);
        }

        String maliciousFileUrl = tempFile.toURI().toString();
        String result = null;
        try {
            result = bookmarkService.fetchTitleFromUrl(maliciousFileUrl);
        } catch (Exception e) {
            // Expected to fail, which is a pass for security.
        }

        Assert.assertFalse(
                "LFI Attack Successful! The service may have read a local file from the filesystem.",
                result != null && result.contains(SECRET_FILE_CONTENT_MARKER)
        );
    }

    /**
     * 测试通过提供一个使用 "file://" 协议的 URL 来模拟 SSRF/LFI（本地文件包含）攻击，并添加url绕过。
     */
    @Test
    public void testSsrAttackWithLocalFileUrlAndBypass() throws IOException {
        File tempFile = tempFolder.newFile("secret.txt");
        try (PrintWriter out = new PrintWriter(new FileWriter(tempFile))) {
            out.println(SECRET_FILE_CONTENT_MARKER);
        }

        String maliciousFileUrl = "url:" + tempFile.toURI().toString();
        String result = null;
        try {
            result = bookmarkService.fetchTitleFromUrl(maliciousFileUrl);
        } catch (Exception e) {
            // Expected to fail, which is a pass for security.
        }


        Assert.assertFalse(
                "LFI Attack Successful! The service may have read a local file from the filesystem via: " + maliciousFileUrl,
                result != null && result.contains(SECRET_FILE_CONTENT_MARKER)
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
    public void testRedirectToInternalNetworkAccess() {
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
     * Helper method to assert that an SSRF attempt is blocked.
     * It calls the service and checks that the fetched title does NOT match the secret title marker.
     * If the request is blocked by an exception, the test is considered passed.
     *
     * @param maliciousUrl      The SSRF payload URL.
     * @param attackDescription A description of the attack vector for logging.
     */
    private void assertSsrfaIsBlocked(String maliciousUrl, String attackDescription) {
        this.httpRequestReceived = false;

        try {
            System.out.println("Testing with SSRF payload (" + attackDescription + "): " + maliciousUrl);
            bookmarkService.fetchTitleFromUrl(maliciousUrl);
        } catch (IOException e) {
            // An IOException (e.g., "Connection refused", "Host not allowed", or timeout)
            // indicates the request was likely blocked by a security measure. This is the
            // expected behavior for a secure system.
            System.out.println("Request may have been blocked by an exception. Verifying no HTTP request was made. Exception: " + e.getMessage());
        } catch (Exception e) {
            // Catch any other exception as a sign that the request was blocked.
            System.out.println("Request may have been blocked by an exception. Verifying no HTTP request was made. Exception: " + e.getMessage());
        }

        Assert.assertFalse(
                "CRITICAL: SSRF Vulnerability Detected! (" + attackDescription + "). The service made a forbidden HTTP request to: " + maliciousUrl,
                this.httpRequestReceived
        );
    }

    /**
     * A simple handler to be called when an SSRF attempt is made.
     * It's only job is to set a flag indicating a request was received.
     */
    private class LocalServerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            SecurityTest.this.httpRequestReceived = true;
            String response = "Request Received";
            byte[] responseBytes = response.getBytes();
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
}
