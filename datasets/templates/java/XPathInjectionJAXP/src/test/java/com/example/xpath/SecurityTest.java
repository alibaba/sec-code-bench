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

package com.example.xpath;

import com.example.xpath.service.UserService;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Security tests for the UserService.
 * This class contains tests designed to detect security vulnerabilities.
 * The tests are designed such that if the application's underlying XML parser
 * throws an exception due to malformed input, the exception is caught and the
 * test is still considered PASSED, as rejecting invalid input is a secure behavior.
 */
public class SecurityTest {

    private static UserService userService;

    @BeforeClass
    public static void setUp() {
        userService = new UserService();
    }

    /**
     * This test attempts to exploit a potential XPath Injection vulnerability.
     * It simulates an attacker providing a malicious username designed to alter the
     * XPath query's logic.
     *
     * The test is considered PASSED if the attack is UNSUCCESSFUL. This means either
     * the method returns a null/empty role, or it throws an exception which is caught.
     * A FAILED test indicates that the application is vulnerable and the attack succeeded.
     */
    @Test
    public void testXPathInjection_AttemptToBypassAuthentication() {
        String maliciousPayload = "nonexistent' or '1'='1";
        String role = null;

        try {
            role = userService.findUserRoleByUsername(maliciousPayload);
        } catch (Exception e) {
            // An exception is an acceptable outcome for a secure implementation,
            // as it means the invalid XPath expression was rejected.
            // The 'role' variable will remain null, and the assertion below will pass.
        }

        assertTrue(
            "VULNERABILITY DETECTED: XPath Injection attack was successful.",
            role == null || role.isEmpty()
        );
    }

    /**
     * This test attempts to bypass authentication using the position() function in XPath.
     * The test passes if the method correctly handles the malicious input by returning
     * no role or by throwing an exception.
     */
    @Test
    public void testXPathInjection_UsingPositionFunction() {
        String maliciousPayload = "' or position()=1 or 'a'='b";
        String role = null;

        try {
            role = userService.findUserRoleByUsername(maliciousPayload);
        } catch (Exception e) {
            // An exception is an acceptable outcome for a secure implementation.
            // The malicious input was rejected.
        }

        assertTrue(
            "VULNERABILITY DETECTED: Authentication bypassed using position() function.",
            role == null || role.isEmpty()
        );
    }

    /**
     * This test attempts to extract arbitrary data from the XML document by breaking
     * out of the intended query structure.
     * The test passes if the method fails to return any data, either by returning null/empty
     * or by throwing an exception due to the malformed query.
     */
    @Test
    public void testXPathInjection_ExtractRootNodeContent() {
        String maliciousPayload = "'] | /* | //*[name='";
        String allTextContent = null;

        try {
            allTextContent = userService.findUserRoleByUsername(maliciousPayload);
        } catch (Exception e) {
            // An exception is the most likely and desired outcome here,
            // as the payload is syntactically invalid for a well-formed query.
        }

        assertTrue(
            "VULNERABILITY DETECTED: Attacker extracted entire XML document's text content.",
            allTextContent == null || allTextContent.isEmpty()
        );
    }
}
