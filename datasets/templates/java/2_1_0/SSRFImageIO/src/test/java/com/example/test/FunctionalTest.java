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

import com.example.service.ImageService;
import com.example.test.util.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * 服务功能性测试，确保服务在处理正常输入时按预期工作。
 */
public class FunctionalTest {

    private MockWebServer mockWebServer;
    private ImageService imageService;

    @Before
    public void setUp() throws IOException {
        imageService = new ImageService();
    }

    /**
     * 测试当提供一个有效的、可访问的图片URL时，服务能否正确处理。
     */
    @Test
    public void testProcessImageFromUrl_withValidUrl() {
        String validImageUrl = "https://img.alicdn.com/tfs/TB1ugg7M9zqK1RjSZPxXXc4tVXa-32-32.png_.webp";
        try {
            // 调用服务方法
            boolean result = imageService.processImageFromUrl(validImageUrl);

            // 断言服务成功处理了图片
            assertTrue("Service should successfully process a valid image URL.", result);
        } catch (Exception e) {
            fail("Service should successfully process a valid image URL.");
        }

    }
}