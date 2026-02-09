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

package com.example.datamodel;

import com.example.datamodel.service.DataProcessingService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;

/**
 * Security tests for the DataProcessingService.
 * These tests attempt to exploit potential vulnerabilities and assert that the attacks are unsuccessful.
 */
public class SecurityTest {

    private DataProcessingService service;
    private HttpServer mockServer;
    private int serverPort;
    private final AtomicBoolean requestReceived = new AtomicBoolean(false);

    /**
     * Finds a free port and starts a mock HTTP server before each test.
     * The server will listen for incoming requests, which indicates a successful SSRF attack via XXE.
     */
    @Before
    public void setUp() throws IOException {
        // Find an available port
        try (ServerSocket s = new ServerSocket(0)) {
            serverPort = s.getLocalPort();
        }

        // Create and start a simple HTTP server
        mockServer = HttpServer.create(new InetSocketAddress("127.0.0.1", serverPort), 0);
        mockServer.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                requestReceived.set(true);
                String response = "OK";
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        });
        mockServer.setExecutor(null); // creates a default executor
        mockServer.start();

        service = new DataProcessingService();
        requestReceived.set(false); // Reset flag before each test
    }

    /**
     * Stops the mock HTTP server after each test.
     */
    @After
    public void tearDown() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    /**
     * Tests for XXE vulnerability by attempting an SSRF attack.
     * The payload tries to force the XML parser to make an HTTP request to a local mock server.
     * The test PASSES if the server does NOT receive a request, indicating the attack was blocked.
     */
    @Test
    public void testXxeSsrAttack() {
        String maliciousPayload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<!DOCTYPE userProfile [" +
                "  <!ENTITY xxe SYSTEM \"http://127.0.0.1:" + serverPort + "/\">" +
                "]>" +
                "<userProfile>" +
                "  <name>&xxe;</name>" + // The external entity is referenced here
                "  <email>pwned@example.com</email>" +
                "  <userId>999</userId>" +
                "</userProfile>";

        // The service call might throw an exception or return null, which is acceptable.
        // The critical part is whether the external entity was resolved.
        try {
            service.processUserProfile(maliciousPayload);
        } catch (Exception e) {
            // Exceptions are expected and can be ignored for this test.
            // The focus is on the side effect (HTTP request).
        }

        // The assertion checks if the attack was successful.
        // A successful attack means requestReceived is true, which should FAIL the test.
        // The test passes if requestReceived is false (attack blocked).
        assertFalse("XXE vulnerability detected: an external HTTP request was made.", requestReceived.get());
    }
}