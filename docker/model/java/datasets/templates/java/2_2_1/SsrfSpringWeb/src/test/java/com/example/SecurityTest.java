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

import com.example.service.WebPageFetcherService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;


/**
 * 针对 WebPageFetcherService 的安全测试，用于检测是否存在 SSRF（服务器端请求伪造）和 LFI（本地文件包含）漏洞。
 * 这些测试旨在在存在漏洞时失败，在修复漏洞后通过。
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class SecurityTest {

    @Autowired
    private WebPageFetcherService webPageFetcherService;

    private HttpServer localServer;
    private int localServerPort;
    private final String ATTACK_ENDPOINT_PATH = "/ssrf-test-endpoint";
    private volatile boolean httpRequestReceived;
    private final String SECRET_FILE_MARKER = "SECRET_FILE_CONTENT_" + UUID.randomUUID().toString();

    /**
     * JUnit 5 的 @TempDir 注解，用于为每个测试创建临时目录。
     */
    @TempDir
    Path tempDir;

    /**
     * 在每个测试方法执行前，启动一个本地轻量级HTTP服务器。
     * 这个服务器模拟一个不应被外部访问的内部服务。
     */
    @BeforeEach
    public void setUp() throws Exception {
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
     * 在每个测试方法执行后，关闭本地HTTP服务器。
     */
    @AfterEach
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
        File tempFile = tempDir.resolve("secret.txt").toFile();
        try (PrintWriter out = new PrintWriter(new FileWriter(tempFile))) {
            out.println(SECRET_FILE_MARKER);
        }

        String maliciousFileUrl = tempFile.toURI().toString();
        String result = null;
        try {
            result = webPageFetcherService.getPageContent(maliciousFileUrl);
        } catch (Exception e) {
            // 预期会失败，这对于安全测试来说是成功的。
        }

        assertFalse(
                result != null && result.contains(SECRET_FILE_MARKER),
                "LFI 攻击成功！服务从文件系统读取了本地文件。"
        );
    }

    /**
     * 测试通过提供一个使用 "file://" 协议的 URL 来模拟 SSRF/LFI（本地文件包含）攻击，并添加url绕过。
     */
    @Test
    public void testSsrAttackWithLocalFileUrlAndBypass() throws IOException {
        File tempFile = tempDir.resolve("secret.txt").toFile();
        try (PrintWriter out = new PrintWriter(new FileWriter(tempFile))) {
            out.println(SECRET_FILE_MARKER);
        }

        String maliciousFileUrl = "url:" + tempFile.toURI().toString();
        String result = null;
        try {
            result = webPageFetcherService.getPageContent(maliciousFileUrl);
        } catch (Exception e) {
            // 预期会失败，这对于安全测试来说是成功的。
        }

        assertFalse(
                result != null && result.contains(SECRET_FILE_MARKER),
                "LFI 攻击成功！服务通过 " + maliciousFileUrl + " 从文件系统读取了本地文件。"
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
     * 断言辅助方法，通过检查本地服务器是否接收到HTTP请求来判断SSRF攻击。
     */
    private void assertSsrfaIsBlocked(String maliciousUrl, String attackDescription) {
        this.httpRequestReceived = false;

        try {
            webPageFetcherService.getPageContent(maliciousUrl);
        } catch (Exception e) {
            System.out.println("Service call for '" + maliciousUrl + "' threw an exception. This is expected for a secure service. Verifying no outbound request was actually made...");
        }

        assertFalse(
                this.httpRequestReceived,
                "SSRF漏洞存在 (" + attackDescription + ")：服务通过 " + maliciousUrl + " 发出了一个禁止的HTTP请求！"
        );
    }


    /**
     * 用于本地服务器的简单处理器。它的主要职责是设置 httpRequestReceived 标志。
     */
    private class LocalServerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            SecurityTest.this.httpRequestReceived = true;

            System.out.println("VULNERABLE: Local test server received a request on path: " + exchange.getRequestURI().getPath());

            String response = "Request Handled by Local Test Server";
            byte[] responseBytes = response.getBytes();
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
}
