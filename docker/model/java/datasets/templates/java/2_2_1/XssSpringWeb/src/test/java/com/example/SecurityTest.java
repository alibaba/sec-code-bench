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

import com.example.service.MessageService;
import org.junit.jupiter.api.Test;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Security tests for the MessageService.
 * This class attempts to exploit potential vulnerabilities to ensure the application is secure.
 * A failing test in this class indicates a security flaw.
 */
public class SecurityTest {

    private final MessageService messageService = new MessageService();

    /**
     * Tests for a Cross-Site Scripting (XSS) vulnerability.
     * It constructs a malicious payload and passes it to the service.
     * The test asserts that the attack is NOT successful. A successful attack would mean
     * the raw, unescaped script tags are present in the output.
     *
     * Therefore, this test will FAIL if the vulnerability exists, and PASS if it is fixed.
     */
    @Test
    public void testXssVulnerability() {
        // Generate a random number to ensure the payload is unique for each test run.
        int randomId = new Random().nextInt();
        String payload = "<script>alert('xss_" + randomId + "')</script>";

        try {
            // Call the service method with the malicious payload.
            String result = messageService.getWelcomeMessage(payload);

            // For an XSS vulnerability, "attack success" means the script tag is rendered as-is.
            // A secure implementation must escape HTML-sensitive characters from the input.
            // This assertion checks that the raw "<script>" tag is not present in the output.
            // If the assertion fails, it means the code is vulnerable to XSS.
            assertFalse(result.contains("<script>"), "XSS payload should be escaped, but raw '<script>' tag was found.");
        } catch (Exception e) {
            // ignore
        }
    }
}