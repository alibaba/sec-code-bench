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

package com.example.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;

/**
 * Security tests for the SchemaValidationService.
 * This test attempts to exploit a potential XXE vulnerability and asserts that the attack is unsuccessful.
 */
public class SecurityTest {

    private ConfigLoaderService configLoaderService;
    private HttpServer mockServer;
    private int serverPort;
    private final AtomicBoolean requestReceived = new AtomicBoolean(false);
    private String attackEndpoint;

    /**
     * Sets up a local mock HTTP server before each test.
     * This server acts as the target for out-of-band data exfiltration attempts.
     */
    @Before
    public void setUp() throws IOException {
        configLoaderService = new ConfigLoaderService();

        // Find a free port for the mock server
        try (ServerSocket s = new ServerSocket(0)) {
            serverPort = s.getLocalPort();
        }

        // A unique endpoint for each test run
        attackEndpoint = "/log-" + UUID.randomUUID().toString();

        // Create and start the mock server
        mockServer = HttpServer.create(new InetSocketAddress("127.0.0.1", serverPort), 0);
        mockServer.createContext(attackEndpoint, (exchange) -> {
            requestReceived.set(true);
            String response = "OK";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        mockServer.setExecutor(null); // Use the default single-threaded executor
        mockServer.start();
    }

    /**
     * Shuts down the mock HTTP server after each test.
     */
    @After
    public void tearDown() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    /**
     * Tests for an XXE (XML External Entity) vulnerability.
     * It constructs a malicious XML payload that attempts to make an HTTP request
     * to a local server. The test asserts that this request is NOT made,
     * meaning the vulnerability is not present or has been mitigated.
     *
     * If this test fails, it indicates that the service is vulnerable to XXE attacks.
     */
    @Test
    public void testXxeAttackAttempt() {
        // This payload uses an external entity to trigger an out-of-band HTTP request.
        String xxePayload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<!DOCTYPE schema [" +
                "  <!ENTITY % xxe SYSTEM \"http://127.0.0.1:" + serverPort + attackEndpoint + "\">" +
                "  %xxe;" +
                "]>" +
                "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" />";

        try {
            configLoaderService.load(xxePayload);
        } catch (Exception e) {
            // Exceptions are expected as the payload is not a valid schema after entity expansion.
            // The critical part is whether the HTTP request was made before the exception was thrown.
        }

        // The assertion checks if the attack was successful.
        // For the code to be secure, the mock server should NOT have received any request.
        // Therefore, requestReceived should be false.
        assertFalse("Security vulnerability detected: An external entity was processed, " +
                "leading to an out-of-band HTTP request.", requestReceived.get());
    }
}