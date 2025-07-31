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

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * FileService安全性测试类
 * 主要测试目录穿越漏洞
 */
public class SecurityTest {
    
    private FileService fileService;
    private String tmpFilename;
    
    @Before
    public void setUp() {
        fileService = new FileService();
        // 通过uuid生成一个随机的文件名
        tmpFilename = UUID.randomUUID().toString() + ".txt";
    }
    
    @After
    public void tearDown() {
        // 删除创建的临时文件
        try {
            Files.deleteIfExists(Paths.get("/tmp/" + tmpFilename));
        } catch (Exception e) {
            // 忽略清理错误
        }
    }
    
    @Test
    public void testDirectoryTraversalVulnerability() {
        // 构造目录穿越路径
        String maliciousPath = "../../../../tmp/" + tmpFilename;
        String content = "This is a test for directory traversal vulnerability";
        
        try {
            // 尝试写入恶意路径
            boolean result = fileService.writeFileContent(maliciousPath, content);
            
            // 检查是否成功写入（如果成功，说明存在漏洞）
            if (result) {
                // 验证文件是否被写入到/tmp目录
                File targetFile = new File("/tmp/" + tmpFilename);
                boolean fileExists = targetFile.exists();
                
                if (fileExists) {
                    fail("发现目录穿越漏洞！文件被写入到/tmp/" + tmpFilename + "，说明路径验证存在安全问题");
                }
            }
            
                    // 如果写入失败，说明没有漏洞，测试通过
        assertFalse("目录穿越攻击应该被阻止", result);
        
    } catch (Exception e) {
        // 如果抛出异常，说明输入验证有效，测试通过
        // 记录异常信息但不让测试失败
        System.out.println("检测到异常，说明输入验证有效: " + e.getMessage());
        // 测试通过，因为异常表明安全机制在工作
    }
}

}
