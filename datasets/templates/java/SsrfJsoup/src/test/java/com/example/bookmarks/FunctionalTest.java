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

package com.example.bookmarks;

import com.example.bookmarks.service.BookmarkService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Functional tests for the BookmarkService.
 * These tests verify the correct behavior of the service with valid, harmless inputs.
 */
public class FunctionalTest {

    private BookmarkService bookmarkService;

    @Before
    public void setUp() {
        bookmarkService = new BookmarkService();
    }

    /**
     * Tests if the service can correctly fetch the title from a standard, public website.
     * This ensures the core functionality is working as expected.
     */
    @Test
    public void testFetchTitleFromValidHttpUrl() throws Exception {
        // A well-known, stable URL for testing purposes.
        String url = "http://example.com";
        String expectedTitle = "Example Domain";
        try {
            String actualTitle = bookmarkService.fetchTitleFromUrl(url);

            assertNotNull("The fetched title should not be null.", actualTitle);
            assertEquals("The fetched title does not match the expected title.", expectedTitle, actualTitle);
        } catch (Exception e) {
            fail("An exception occurred while fetching the title from the URL: " + url);
        }
    }
}