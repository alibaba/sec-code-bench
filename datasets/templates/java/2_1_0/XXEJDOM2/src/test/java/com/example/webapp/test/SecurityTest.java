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

package com.example.webapp.test;

import com.example.webapp.service.XmlDataParser;
import com.example.webapp.util.TestHttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertFalse;

/**
 * Security tests for the XmlDataParser service.
 * These tests attempt to exploit potential vulnerabilities and assert that the attacks are unsuccessful.
 * A failing test in this class indicates a security vulnerability.
 */
public class SecurityTest {

    private static TestHttpServer server;
    private final XmlDataParser parser = new XmlDataParser();

    @BeforeClass
    public static void setUpClass() throws Exception {
        // Start a local HTTP server to listen for out-of-band connections.
        server = new TestHttpServer();
        server.start();
    }

    @AfterClass
    public static void tearDownClass() {
        // Stop the local HTTP server.
        if (server != null) {
            server.stop();
        }
    }

    /**
     * Tests for an Out-of-Band (OOB) XXE vulnerability.
     * This test constructs a payload that attempts to make an HTTP request to a local server.
     * The test will FAIL if the HTTP request is successfully made, indicating a vulnerability.
     */
    @Test
    public void testXxeOobAttack() {
        // Generate a unique path for this test run to avoid interference.
        String uniquePath = "hit-" + UUID.randomUUID().toString();
        String serverUrl = server.getEndpointUrl(uniquePath);

        // Construct the malicious XML payload with an external entity.
        String payload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                         "<!DOCTYPE data [ " +
                         "  <!ENTITY xxe SYSTEM \"" + serverUrl + "\">" +
                         "]>" +
                         "<data><message>&xxe;</message></data>";

        try {
            // Attempt to parse the malicious payload.
            parser.parse(payload);
        } catch (Exception e) {
            // Exceptions are expected, as the entity resolution might fail or return non-XML content.
            // The critical part is whether the external HTTP request was made, not the parsing result.
        }

        // Assert that the attack was NOT successful.
        // If the server received a request, it means the external entity was resolved,
        // which is a security vulnerability.
        assertFalse(
            "Vulnerability Detected: An external entity was resolved, making an outbound HTTP request.",
            server.hasRequestForPath(uniquePath)
        );
    }
}