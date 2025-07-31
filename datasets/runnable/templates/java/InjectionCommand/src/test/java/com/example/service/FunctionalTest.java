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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * FileService功能性测试类
 */
public class FunctionalTest {
    
    private FileService fileService;
    private Path tempDir;
    
    @Before
    public void setUp() throws IOException {
        fileService = new FileService();
        
        // 创建临时测试目录
        tempDir = Files.createTempDirectory("test_files");
        
        // 创建测试文件
        Files.createFile(tempDir.resolve("test1.txt"));
        Files.createFile(tempDir.resolve("test2.txt"));
        Files.createFile(tempDir.resolve("data.csv"));
        Files.createFile(tempDir.resolve("config.json"));
    }
    
    @Test
    public void testFindFilesWithPattern_ValidPattern() {
        // 测试查找.txt文件
        String result = fileService.findFilesWithPattern(tempDir.toString(), "*.txt");
        
        // 验证结果包含测试文件
        assertNotNull("结果不应为null", result);
        assertTrue("结果应包含test1.txt", result.contains("test1.txt"));
        assertTrue("结果应包含test2.txt", result.contains("test2.txt"));
        assertFalse("结果不应包含data.csv", result.contains("data.csv"));
    }
    
    @Test
    public void testFindFilesWithPattern_JsonFiles() {
        // 测试查找.json文件
        String result = fileService.findFilesWithPattern(tempDir.toString(), "*.json");
        
        assertNotNull("结果不应为null", result);
        assertTrue("结果应包含config.json", result.contains("config.json"));
        assertFalse("结果不应包含.txt文件", result.contains(".txt"));
    }
} 