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
package com.example.service;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * VelocityTemplateService测试类
 */
public class FunctionalTest {
    
    private VelocityTemplateService velocityTemplateService;
    
    @Before
    public void setUp() {
        velocityTemplateService = new VelocityTemplateService();
    }
    
    @Test
    public void testRenderUserDashboard_SimpleTemplate() {
        // 准备测试数据
        String userId = "testUser123";
        String template = "#set($user = $userId) Hello $user!";
        
        // 执行测试
        String result = velocityTemplateService.renderUserDashboard(userId, template);
        
        // 验证结果
        assertNotNull("结果不应为空", result);
        assertTrue("结果应包含用户ID", result.contains("testUser123"));
        assertEquals("Hello testUser123!", result.trim());
    }
    
    @Test
    public void testRenderUserDashboard_ComplexTemplate() {
        // 准备测试数据
        String userId = "admin001";
        String template = "<div class=\"dashboard\">\n" +
                "<h1>欢迎，用户 $userId</h1>\n" +
                "<div class=\"stats\">\n" +
                "<p>用户ID: $userId</p>\n" +
                "<p>登录时间: $date</p>\n" +
                "</div>\n" +
                "</div>";
        
        // 执行测试
        String result = velocityTemplateService.renderUserDashboard(userId, template);
        
        // 验证结果
        assertNotNull("结果不应为空", result);
        assertTrue("结果应包含用户ID", result.contains("admin001"));
        assertTrue("结果应包含HTML标签", result.contains("<div"));
        assertTrue("结果应包含标题", result.contains("<h1>"));
    }
} 