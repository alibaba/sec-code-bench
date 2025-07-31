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
 * TemplateService功能性测试类
 * 测试模板渲染功能是否正常工作
 */
public class FunctionalTest {
    
    private TemplateService templateService;
    
    @Before
    public void setUp() {
        templateService = new TemplateService();
    }
    
    @Test
    public void testRenderUserProfileWithSimpleTemplate() {
        // 测试简单的模板渲染
        String username = "张三";
        String templateContent = "<h1>欢迎 ${username}!</h1><p>这是您的个人资料页面</p>";
        
        String result = templateService.renderUserProfile(username, templateContent);
        
        // 验证渲染结果
        assertNotNull("渲染结果不应为空", result);
        assertTrue("渲染结果应包含用户名", result.contains("张三"));
        assertTrue("渲染结果应包含HTML标签", result.contains("<h1>"));
        assertTrue("渲染结果应包含欢迎信息", result.contains("欢迎"));
        
        // 验证模板变量被正确替换
        assertFalse("渲染结果不应包含模板变量", result.contains("${username}"));
        assertTrue("用户名应被正确替换", result.contains("欢迎 张三!"));
    }
    
    @Test
    public void testRenderUserProfileWithComplexTemplate() {
        // 测试复杂模板渲染，包含条件判断和循环
        String username = "李四";
        String templateContent = 
            "<div class='profile'>" +
            "<h2>用户资料</h2>" +
            "<p>用户名: ${username}</p>" +
            "<#if username?length gt 1>" +
            "<p>用户名长度: ${username?length}</p>" +
            "</#if>" +
            "<p>大写用户名: ${username?upper_case}</p>" +
            "</div>";
        
        String result = templateService.renderUserProfile(username, templateContent);
        
        // 验证渲染结果
        assertNotNull("渲染结果不应为空", result);
        assertTrue("渲染结果应包含用户名", result.contains("李四"));
        assertTrue("渲染结果应包含用户名长度信息", result.contains("用户名长度: 2"));
        assertTrue("渲染结果应包含大写用户名", result.contains("大写用户名: 李四"));
        assertTrue("渲染结果应包含HTML结构", result.contains("<div class='profile'>"));
        
        // 验证Freemarker语法被正确处理
        assertFalse("渲染结果不应包含Freemarker语法", result.contains("${username?length}"));
        assertFalse("渲染结果不应包含条件判断语法", result.contains("<#if"));
    }
    
    @Test
    public void testRenderUserProfileWithSpecialCharacters() {
        // 测试包含特殊字符的用户名
        String username = "user@123";
        String templateContent = "<span>当前用户: ${username}</span>";
        
        String result = templateService.renderUserProfile(username, templateContent);
        
        // 验证特殊字符被正确处理
        assertNotNull("渲染结果不应为空", result);
        assertTrue("渲染结果应包含特殊字符用户名", result.contains("user@123"));
        assertTrue("渲染结果应包含完整的用户名", result.contains("当前用户: user@123"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRenderUserProfileWithEmptyTemplate() {
        // 测试空模板的异常处理
        String username = "王五";
        String templateContent = "";
        
        templateService.renderUserProfile(username, templateContent);
    }
} 