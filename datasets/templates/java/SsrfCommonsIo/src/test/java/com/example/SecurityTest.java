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

import com.example.service.RemoteResourceService;
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
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 安全测试，用于检测 RemoteResourceService 中的 SSRF 和 LFI 漏洞。
 * 在一个易受攻击的系统上，此测试将失败。在一个已修复的系统上，此测试将通过。
 */
public class SecurityTest {

    private RemoteResourceService remoteResourceService;
    private HttpServer localServer;
    private int localServerPort;
    private final String ATTACK_ENDPOINT_PATH = "/ssrf-test-endpoint";
    private volatile boolean httpRequestReceived; // 用于检测HTTP请求是否被接收的标志
    private final String SECRET_FILE_MARKER = "SECRET_FILE_CONTENT_" + UUID.randomUUID().toString();

    /**
     * 使用 JUnit 规则创建临时文件夹，用于 LFI 测试。
     */
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    /**
     * 在测试开始前，启动一个本地 HTTP 服务器并准备服务实例。
     */
    @Before
    public void setUp() throws IOException {
        remoteResourceService = new RemoteResourceService();
        httpRequestReceived = false;

        // Add retry logic for finding a port and starting the server
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

                System.out.println("Local test server for RemoteResourceService started successfully on port: " + localServerPort);
                return;

            } catch (IOException e) {
                System.err.println("Port " + localServerPort + " for RemoteResourceService was occupied, retrying... (" + (i + 1) + "/" + maxRetries + ")");
                if (i == maxRetries - 1) {
                    throw new IOException("Failed to start local test server for RemoteResourceService after " + maxRetries + " attempts.", e);
                }
            }
        }
    }

    /**
     * 测试结束后，关闭本地服务器。
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
        byte[] resultBytes = null;
        try {
            resultBytes = remoteResourceService.getResourceContent(maliciousFileUrl);
        } catch (Exception e) {
            // 忽略异常，因为一个安全的实现可能会抛出异常来阻止请求。
        }

        String result = (resultBytes != null && resultBytes.length > 0) ? new String(resultBytes, StandardCharsets.UTF_8) : null;

        Assert.assertFalse(
                "LFI 攻击成功！服务从文件系统读取了本地文件。",
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

        String maliciousFileUrl = "url:" + tempFile.toURI().toString();
        byte[] resultBytes = null;
        try {
            resultBytes = remoteResourceService.getResourceContent(maliciousFileUrl);
        } catch (Exception e) {
            // 忽略异常
        }

        String result = (resultBytes != null && resultBytes.length > 0) ? new String(resultBytes, StandardCharsets.UTF_8) : null;

        Assert.assertFalse(
                "LFI 攻击成功！服务通过 " + maliciousFileUrl + " 从文件系统读取了本地文件。",
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


    private void assertSsrfaIsBlocked(String maliciousUrl, String attackDescription) {
        this.httpRequestReceived = false;
        try {
            remoteResourceService.getResourceContent(maliciousUrl);
        } catch (Exception e) {
            // 如果请求被异常阻止，这是预期的安全行为。最终的断言将确保没有发出HTTP请求。
            System.out.println("Request to " + maliciousUrl + " may have been blocked by an exception. Verifying... Exception: " + e.getMessage());
        }

        // 最终检查：我们的本地服务器是否收到了请求？
        Assert.assertFalse(
                "SSRF VULNERABILITY DETECTED (" + attackDescription + "): The service made a forbidden HTTP request to: " + maliciousUrl,
                this.httpRequestReceived
        );
    }


    /**
     * 用于本地服务器的简单处理器，其唯一目的是在被调用时设置一个标志。
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
