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

package com.app.test;

import com.app.service.XmlProcessingService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;

/**
 * Security tests to verify that the application is not vulnerable to XXE attacks.
 */
public class SecurityTest {

    private HttpServer mockServer;
    private int serverPort;
    private final AtomicBoolean requestReceived = new AtomicBoolean(false);
    private final String attackPath = "/" + UUID.randomUUID().toString();

    /**
     * Sets up a local mock HTTP server before each test.
     * The server listens on a random free port to avoid conflicts.
     */
    @Before
    public void setUp() throws IOException {
        requestReceived.set(false);
        mockServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serverPort = mockServer.getAddress().getPort();
        mockServer.createContext(attackPath, exchange -> {
            requestReceived.set(true);
            String response = "Acknowledged";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        mockServer.setExecutor(null);
        mockServer.start();
    }

    /**
     * Shuts down the mock server after each test.
     */
    @After
    public void tearDown() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    /**
     * Tests for XXE vulnerability by providing a malicious XML payload.
     * The payload attempts to trigger an outbound HTTP request to a local mock server.
     * The test will PASS if the request is NOT received, indicating the vulnerability is mitigated.
     * The test will FAIL if the request IS received, indicating the vulnerability is present.
     */
    @Test
    public void testLoadSchemaWithXxePayload() {
        // This payload uses an external parameter entity to trigger an HTTP request.
        // A vulnerable XML parser will resolve this entity and contact the URL.
        String maliciousPayload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<!DOCTYPE root [" +
                "  <!ENTITY % xxe SYSTEM \"http://127.0.0.1:" + serverPort + attackPath + "\">" +
                "  %xxe;" +
                "]>" +
                // A minimal schema structure is included to satisfy the parser.
                "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>";

        XmlProcessingService service = new XmlProcessingService();
        try {
            service.loadSchema(maliciousPayload);
        } catch (Exception e) {
            // An exception is expected because the payload is not a valid schema
            // or because external entity loading is blocked.
            // The crucial part is to check the side effect (the HTTP request)
            // regardless of whether an exception was thrown.
        }

        // This assertion is the core of the security test.
        // It fails if the mock server was contacted, which proves the XXE vulnerability exists.
        // A passing test indicates that external entities were not processed.
        assertFalse("Security Alert: XXE attack was successful. An external HTTP request was triggered.", requestReceived.get());
    }
}