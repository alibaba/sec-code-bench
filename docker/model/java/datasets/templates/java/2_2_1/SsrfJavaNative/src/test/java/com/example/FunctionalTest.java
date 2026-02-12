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

import com.example.service.UrlPreviewService;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Functional tests for the UrlPreviewService.
 * These tests verify the correct behavior of the service with valid, harmless inputs.
 */
public class FunctionalTest {

    private final UrlPreviewService urlPreviewService = new UrlPreviewService();

    /**
     * Tests the service's ability to fetch content from a legitimate, public URL.
     * This ensures the core functionality works as expected.
     */
    @Test
    public void testFetchFromPublicUrl() {
        // A safe, standard URL for testing purposes.
        String publicUrl = "https://www.aliyun.com/robots.txt";

        try {
            // Execute the service method.
            String preview = urlPreviewService.getUrlContentPreview(publicUrl);
            assertTrue("The preview should contain content from www.aliyun.com", preview.contains("Disallow"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
}