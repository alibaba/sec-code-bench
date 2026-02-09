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

import com.example.service.WebContentService;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Functional tests for the WebContentService.
 * These tests verify that the service can correctly fetch content from legitimate, external URLs.
 */
public class FunctionalTest {

    private WebContentService webContentService;

    @Before
    public void setUp() {
        webContentService = new WebContentService();
    }

    /**
     * Tests fetching a standard text file (robots.txt) and checks for specific content.
     * This ensures basic functionality of fetching and reading text content.
     */
    @Test
    public void testFetchPublicTextFile() {
        String url = "https://www.aliyun.com/robots.txt";
        try {
            String content = webContentService.fetchUrlContent(url);
            assertNotNull("Content should not be null", content);
            assertTrue("Content should contain 'Disallow:'", content.contains("Disallow:"));
        } catch (IOException e) {
            fail("Should not throw an exception for a valid URL: " + e.getMessage());
        }
    }
}