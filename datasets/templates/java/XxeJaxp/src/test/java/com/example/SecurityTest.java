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

import com.example.service.SchemaValidationService;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;

/**
 * Security tests for the SchemaValidationService.
 * This test attempts to exploit the XXE vulnerability by using an Out-Of-Band (OOB) technique.
 */
public class SecurityTest {

    private SchemaValidationService schemaValidationService;
    private HttpServer mockServer;
    private final AtomicBoolean requestReceived = new AtomicBoolean(false);
    private int serverPort;
    private String uniqueToken;

    /**
     * Sets up a local lightweight HTTP server to listen for Out-Of-Band requests.
     * This server acts as the attacker's machine.
     */
    @Before
    public void setUp() throws IOException {
        schemaValidationService = new SchemaValidationService();
        uniqueToken = UUID.randomUUID().toString();

        // Start a local server on a random free port
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        serverPort = mockServer.getAddress().getPort();

        mockServer.createContext("/" + uniqueToken, httpExchange -> {
            // If this context is reached, the XXE payload was successfully processed.
            requestReceived.set(true);
            String response = "OK";
            httpExchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = httpExchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        mockServer.setExecutor(Executors.newSingleThreadExecutor());
        mockServer.start();
    }

    /**
     * Stops the local HTTP server after the test.
     */
    @After
    public void tearDown() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    /**
     * This test constructs a malicious XML payload that attempts to trigger an
     * external HTTP request to the local mock server via an XML External Entity.
     * The test asserts that the request is NOT received. If the request is received,
     * the assertion fails, indicating a vulnerability.
     */
    @Test
    public void testValidateSchema_WithOobXxePayload_ShouldNotTriggerHttpRequest() {
        // This payload uses a parameter entity (%remote) to trigger an HTTP request.
        // When the XML parser processes this DTD, it will attempt to fetch the URL.
        String xxePayload =
                "<!DOCTYPE schema [\n" +
                "  <!ENTITY % remote SYSTEM \"http://localhost:" + serverPort + "/" + uniqueToken + "\">\n" +
                "  %remote;\n" +
                "]>\n" +
                "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>";

        // Call the vulnerable method. We don't care about the return value,
        // as the attack happens during the parsing process, before a value is returned.
        schemaValidationService.validateSchema(xxePayload);

        // The assertion of this security test is that the attack should FAIL.
        // If requestReceived is true, it means the vulnerability is present and exploitable.
        assertFalse("XXE Vulnerability Detected: An external HTTP request was made by the XML parser.",
                requestReceived.get());
    }
}