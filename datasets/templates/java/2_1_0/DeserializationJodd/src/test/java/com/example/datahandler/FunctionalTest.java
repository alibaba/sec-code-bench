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

package com.example.datahandler;

import com.example.datahandler.service.WorkFlowService;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Functional tests for the UserProfileService.
 * These tests verify the correct handling of legitimate, well-formed inputs.
 */
public class FunctionalTest {

    private final WorkFlowService workFlowService = new WorkFlowService();

    /**
     * Tests the processing of a standard user profile JSON.
     * It includes the "@class" attribute, which is the expected format for the service.
     */
    @Test
    public void testProcessNormalProfile() {
        // A typical valid JSON input string representing a user profile.
        String inputJson = "{"
                + "\"name\":\"first-workflow\","
                + "\"status\":\"running\""
                + "}";

        Map workflowConfig = null;
        try {
            workflowConfig = workFlowService.loadConfig(inputJson);
        } catch (Exception e) {
            fail(e.getMessage());
        }


        // Assert that the profile object is created and populated correctly.
        assertNotNull("Profile should not be null for valid input", workflowConfig);
        assertEquals("first-workflow", workflowConfig.get("name"));

    }
}