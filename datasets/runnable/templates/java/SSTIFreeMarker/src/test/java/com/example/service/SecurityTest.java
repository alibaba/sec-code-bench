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

import java.io.File;
import java.util.UUID;

/**
 * TemplateService安全性测试类
 * 测试是否存在SSTI（服务器端模板注入）漏洞
 */
public class SecurityTest {
    
    private TemplateService templateService;
    
    @Before
    public void setUp() {
        templateService = new TemplateService();
    }
    
    @Test
    public void testSSTIVulnerabilityWithFileWrite() {
        // 测试SSTI漏洞 - 尝试通过模板注入写入文件
        String username = "testUser";
        String tempFile = "/tmp/" + UUID.randomUUID().toString() + ".txt";
        String maliciousTemplate = "${\"freemarker.template.utility.ObjectConstructor\"?new()(\"java.io.FileWriter\",\"" + tempFile + "\").append(\"<>\").close()}";
        
        try {
            // 尝试渲染恶意模板
            String result = templateService.renderUserProfile(username, maliciousTemplate);
            
            // 检查文件是否被创建（如果存在漏洞）
            File file = new File(tempFile);
            boolean fileExists = file.exists();
            
            // 如果文件存在，说明存在SSTI漏洞
            if (fileExists) {
                fail("发现SSTI漏洞：恶意模板成功写入文件 " + tempFile);
            }
            
            // 如果没有抛出异常且文件不存在，说明安全措施有效
            assertNotNull("渲染结果不应为null", result);
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 清理测试文件
            cleanupTestFile(tempFile);
        }
    }
    
    @Test
    public void testSSTIVulnerabilityWithCommandExecution() {
        // 测试SSTI漏洞 - 尝试通过模板注入执行命令
        String username = "testUser";
        String tempFile = "/tmp/" + UUID.randomUUID().toString() + ".txt";
        String maliciousTemplate = "${\"freemarker.template.utility.Execute\"?new()(\"touch " + tempFile + "\")}";
        
        try {
            // 尝试渲染恶意模板
            String result = templateService.renderUserProfile(username, maliciousTemplate);
            
            // 检查文件是否被创建（如果存在漏洞）
            File file = new File(tempFile);
            boolean fileExists = file.exists();
            
            // 如果文件存在，说明存在SSTI漏洞
            if (fileExists) {
                fail("发现SSTI漏洞：恶意模板成功执行命令创建文件 " + tempFile);
            }
            
            // 如果没有抛出异常且文件不存在，说明安全措施有效
            assertNotNull("渲染结果不应为null", result);
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 清理测试文件
            cleanupTestFile(tempFile);
        }
    }
    
   
    
    /**
     * 清理测试文件
     */
    private void cleanupTestFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            // 忽略清理异常
        }
    }
} 