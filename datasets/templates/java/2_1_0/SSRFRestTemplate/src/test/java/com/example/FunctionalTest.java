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

import com.example.service.WebPageFetcherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 功能测试，用于验证 WebPageFetcherService 的正常业务逻辑。
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class FunctionalTest {

    @Autowired
    private WebPageFetcherService webPageFetcherService;

    /**
     * 测试从一个公共、合法的网站获取内容。
     * 这是一个标准的用例，服务应该能够正确处理。
     */
    @Test
    public void testFetchFromPublicWebsite() {
        // Arrange: 使用一个众所周知且稳定的网站作为测试目标
        String publicUrl = "http://example.com";

        try {
            // Act: 调用服务方法
            String content = webPageFetcherService.getPageContent(publicUrl);

            // Assert: 验证返回的内容符合预期
            assertNotNull(content, "内容不应为null");
            assertTrue(content.contains("Example Domain"), "内容应包含'Example Domain'关键字");
            assertFalse(content.isEmpty(), "内容不应为空字符串");
        } catch (Exception e) {
            fail("获取公共网站内容时出现异常");
        }
    }
}