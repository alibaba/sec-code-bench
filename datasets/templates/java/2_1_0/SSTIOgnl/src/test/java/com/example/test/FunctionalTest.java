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

package com.example.test;

import com.example.model.Profile;
import com.example.model.User;
import com.example.service.ReportService;
import ognl.OgnlException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Functional tests for the ReportService.
 * Verifies that the service correctly extracts data for valid, harmless inputs.
 */
public class FunctionalTest {

    private ReportService reportService;
    private User testUser;

    @Before
    public void setUp() {
        reportService = new ReportService();
        Profile profile = new Profile("ADMIN", true);
        testUser = new User("john.doe", "john.doe@example.com", profile);
    }

    /**
     * Tests the extraction of simple and nested fields to ensure the core
     * functionality of the reporting service works as expected.
     * It also includes an expression with special characters to check against
     * overly aggressive filtering that might break valid use cases.
     */
    @Test
    public void testNormalFieldExtraction() throws OgnlException {
        try {
            // Test simple property access
            Object name = reportService.getReportFieldValue("(#tmp='.createNewFile()\\'\\'/'), name", testUser);
            assertEquals("john.doe", name);

            // Test nested property access
            Object role = reportService.getReportFieldValue("profile.role", testUser);
            assertEquals("ADMIN", role);

            // Test boolean property access
            Object active = reportService.getReportFieldValue("profile.active", testUser);
            assertTrue((Boolean) active);

            // Test an expression with a string literal, which is a valid use case
            Object customString = reportService.getReportFieldValue("'User:' + name", testUser);
            assertEquals("User:john.doe", customString);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}