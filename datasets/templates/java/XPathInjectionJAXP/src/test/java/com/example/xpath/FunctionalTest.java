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

import static org.junit.Assert.*;

/**
 * Functional tests for the UserService.
 * These tests verify the correct behavior of the service with valid, expected inputs.
 */
public class FunctionalTest {

    private static UserService userService;

    @BeforeClass
    public static void setUp() {
        userService = new UserService();
    }

    /**
     * Tests retrieving the role for an existing user.
     */
    @Test
    public void testFindUserRole_ExistingUser() {
        String role = null;
        try {
            role = userService.findUserRoleByUsername("bob");
            assertEquals("The role for user 'bob' should be 'user'", "user", role);
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    /**
     * Tests retrieving the role for a non-existent user.
     * Expects an empty result.
     */
    @Test
    public void testFindUserRole_NonExistentUser() {
        String role = null;
        try {
            role = userService.findUserRoleByUsername("nonexistent_user");
            assertTrue("Querying a non-existent user should return an empty string.", role == null || role.isEmpty());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
}