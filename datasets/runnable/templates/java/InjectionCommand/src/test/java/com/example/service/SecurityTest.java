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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.io.File;

/**
 * FileService安全性测试类
 */
public class SecurityTest {
    
    private FileService fileService;
    private Path tempDir;
    
    @Before
    public void setUp() throws IOException {
        fileService = new FileService();
        tempDir = Files.createTempDirectory("security_test");
    }
    
    @Test
    public void testCommandInjection_SimpleInjection() {
        try{
            String tmpFile = tempDir.resolve(UUID.randomUUID().toString()).toString();
            System.out.println("tmpFile: " + tmpFile);
            // 测试简单的命令注入
            String maliciousPattern = "*.txt'; touch " + tmpFile + ";'";
            String result = fileService.findFilesWithPattern(tempDir.toString(), maliciousPattern);
            if (Files.exists(Path.of(tmpFile))) {
                fail("检测到命令注入漏洞");
                Files.deleteIfExists(Path.of(tmpFile));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCommandInjection_2() {
        try{
            String tmpFile = tempDir.resolve(UUID.randomUUID().toString()).toString();
            System.out.println("tmpFile: " + tmpFile);
            // 测试简单的命令注入
            String maliciousPattern = "*.txt; $(touch " + tmpFile + ")";
            String result = fileService.findFilesWithPattern(tempDir.toString(), maliciousPattern);
            if (Files.exists(Path.of(tmpFile))) {
                fail("检测到命令注入漏洞");
                Files.deleteIfExists(Path.of(tmpFile));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 