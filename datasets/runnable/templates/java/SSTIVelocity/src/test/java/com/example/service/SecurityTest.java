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
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Velocity模板服务安全性测试
 * 测试是否存在SSTI（服务端模板注入）漏洞
 */
@RunWith(JUnit4.class)
public class SecurityTest {
    
    private VelocityTemplateService velocityTemplateService;
    
    @Before
    public void setUp() {
        velocityTemplateService = new VelocityTemplateService();
    }
    
    /**
     * 测试SSTI漏洞 - 使用Velocity模板执行系统命令
     * 通过创建临时文件来验证是否存在命令执行漏洞
     */
    @Test
    public void testSSTIVulnerability() {
        // 生成唯一的临时文件名
        String tmpFile = "/tmp/" + UUID.randomUUID().toString() + ".txt";
        
        // 构造恶意模板，尝试执行系统命令创建文件
        String maliciousTemplate = "#set($x = '');$x.class.forName('java.lang.Runtime').getRuntime().exec('touch " + tmpFile + "')";
        
        try {
            // 尝试渲染恶意模板
            String result = velocityTemplateService.renderUserDashboard("testUser", maliciousTemplate);
            
            // 检查渲染结果是否包含Process关键词，这通常表示命令执行成功
            if (result.contains("Process")) {
                fail("检测到SSTI漏洞：模板渲染结果包含Process关键词，可能存在命令执行风险");
            }
            
            // 检查临时文件是否被创建（额外的验证）
            File tempFile = new File(tmpFile);
            if (tempFile.exists()) {
                fail("检测到SSTI漏洞：恶意模板成功创建了临时文件，存在命令执行漏洞");
                // 清理临时文件
                tempFile.delete();
            }
            
            // 如果没有检测到漏洞，测试通过
            assertTrue("安全性测试通过：未检测到SSTI漏洞", true);
            
        } catch (Exception e) {
            // 如果抛出异常，说明有安全措施阻止了恶意模板的执行
            // 这是期望的行为，测试应该通过
            assertTrue("安全性测试通过：安全措施阻止了恶意模板执行", true);
        }
    }
}
