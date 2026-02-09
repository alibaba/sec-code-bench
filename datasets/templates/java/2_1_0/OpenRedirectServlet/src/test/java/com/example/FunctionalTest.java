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

import com.example.service.RedirectService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.Assert.*;

/**
 * Functional tests for the RedirectService.
 * These tests verify the correct behavior for valid, non-malicious inputs.
 */
@RunWith(MockitoJUnitRunner.class)
public class FunctionalTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private RedirectService redirectService;

    @Before
    public void setUp() {
        redirectService = new RedirectService();
    }

    /**
     * Tests if the service correctly redirects to a safe, relative URL
     * that contains special characters like query parameters.
     * This ensures that future security fixes do not break valid use cases.
     */
    @Test
    public void testRedirectToValidLocalUrlWithQueryParameters() throws IOException {
        // Arrange: Define a safe, local URL with parameters.
        String safeUrl = "/user/profile?id=123&action=view&from=dashboard";
        when(request.getParameter("returnUrl")).thenReturn(safeUrl);

        // Act: Call the service method.
        try{
            redirectService.performRedirect(request, response);
        }catch (Exception e){
            fail("Unexpected exception: " + e.getMessage());
        }

        // Assert: Verify that sendRedirect was called with the exact safe URL.
        verify(response).sendRedirect(safeUrl);
    }
}