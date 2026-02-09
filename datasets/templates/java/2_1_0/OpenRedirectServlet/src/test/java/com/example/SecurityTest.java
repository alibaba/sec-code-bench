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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Security tests for the RedirectService.
 * This test simulates an Open Redirect attack and asserts that the application
 * behaves securely, i.e., it prevents the malicious redirection.
 */
@RunWith(MockitoJUnitRunner.class)
public class SecurityTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    // 使用 @Captor 注解来自动创建 ArgumentCaptor 实例
    @Captor
    private ArgumentCaptor<String> redirectUrlCaptor;

    private RedirectService redirectService;

    @Before
    public void setUp() {
        redirectService = new RedirectService();
    }

    /**
     * This test attempts to perform an open redirect.
     * It provides a malicious external URL as the 'returnUrl' parameter.
     *
     * The test asserts that the application does NOT redirect to the malicious URL.
     * Instead, it should redirect to any other safe URL.
     *
     * NOTE: This test WILL FAIL on the currently vulnerable code.
     * A successful test run indicates that the vulnerability has been fixed.
     */
    @Test
    public void testRedirectWithMaliciousUrlShouldBeBlocked() throws IOException {
        // Arrange: Construct a malicious payload pointing to an external domain.
        String maliciousPayload = "http://evil-phishing-site-" + UUID.randomUUID().toString() + ".com";
        when(request.getParameter("returnUrl")).thenReturn(maliciousPayload);

        // Act: Call the service method with the malicious input.
        redirectService.performRedirect(request, response);

        // Assert: Capture the argument passed to sendRedirect and then assert on its value.
        // 1. Verify sendRedirect was called once and capture the URL it was called with.
        verify(response).sendRedirect(redirectUrlCaptor.capture());

        // 2. Get the captured URL.
        String actualRedirectUrl = redirectUrlCaptor.getValue();

        // 3. Assert that the captured URL is not the malicious one.
        Assert.assertNotEquals("The application should not redirect to the malicious URL", maliciousPayload, actualRedirectUrl);
    }
}
