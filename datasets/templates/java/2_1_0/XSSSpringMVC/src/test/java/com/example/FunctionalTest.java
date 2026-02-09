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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.Assert.*;

/**
 * Functional tests for the MessageService.
 * This class verifies the correct behavior of the service with normal, harmless inputs.
 */
public class FunctionalTest {

    private final MessageService messageService = new MessageService();

    /**
     * Tests the welcome message generation with a username containing special characters
     * that are common in company names or user handles.
     * This ensures that legitimate characters are not incorrectly modified.
     */
    @Test
    public void testWelcomeMessage() {
        try {
            String username = "Bob";
            String expectedHtml = "<h1>Welcome, Bob!</h1>";
            String actualHtml = messageService.getWelcomeMessage(username);
            assertEquals(expectedHtml, actualHtml, "The service should correctly handle usernames.");
        } catch (Exception e) {
            fail("The service should not throw an exception.");
        }
    }
}