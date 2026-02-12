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

import com.example.datamodel.model.UserProfile;
import com.example.datamodel.service.DataProcessingService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Functional tests for the DataProcessingService.
 * These tests verify the correct handling of valid and well-formed inputs.
 */
public class FunctionalTest {

    private DataProcessingService service;

    @Before
    public void setUp() {
        service = new DataProcessingService();
    }

    /**
     * Tests the processing of a standard, valid user profile XML.
     */
    @Test
    public void testProcessNormalUserProfile() {
        String normalXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<userProfile>" +
                "  <name>John Doe</name>" +
                "  <email>john.doe@example.com</email>" +
                "  <userId>12345</userId>" +
                "</userProfile>";

        UserProfile profile = null;
        try {
            profile = service.processUserProfile(normalXml);
        } catch (Exception e) {
            fail("Exception occurred while processing valid XML");
        }

        assertNotNull("Profile should not be null for valid XML", profile);
        assertEquals("Name should be parsed correctly", "John Doe", profile.getName());
        assertEquals("Email should be parsed correctly", "john.doe@example.com", profile.getEmail());
        assertEquals("User ID should be parsed correctly", 12345L, profile.getUserId());
    }

    /**
     * Tests the processing of an XML containing special characters that require encoding.
     * This ensures the parser correctly handles XML entities.
     */
    @Test
    public void testProcessUserProfileWithSpecialChars() {
        String specialCharXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<userProfile>" +
                "  <name>O'Malley &amp; Sons</name>" +
                "  <email>contact-us@omalley-sons.co.uk</email>" +
                "  <userId>54321</userId>" +
                "</userProfile>";

        UserProfile profile = null;
        try {
            profile = service.processUserProfile(specialCharXml);
        } catch (Exception e) {
            fail("Exception occurred while processing XML with special characters");
        }

        assertNotNull("Profile should not be null for XML with special characters", profile);
        assertEquals("Name with special characters should be parsed correctly", "O'Malley & Sons", profile.getName());
        assertEquals("Email should be parsed correctly", "contact-us@omalley-sons.co.uk", profile.getEmail());
    }
}