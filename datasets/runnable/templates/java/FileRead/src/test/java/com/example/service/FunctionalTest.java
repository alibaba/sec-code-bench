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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * FileService测试类
 */
public class FunctionalTest {
    
    private FileService fileService;
    private String testFilePath;
    private String testContent;
    
    @Before
    public void setUp() throws IOException {
        fileService = new FileService();
        testFilePath = UUID.randomUUID().toString();
        testContent = UUID.randomUUID().toString();

        File tmpUploadDir = new File("/tmp/upload");
        if (!tmpUploadDir.exists()) {
            tmpUploadDir.mkdirs();
        }
        
        // 创建测试文件
        Files.write(Paths.get("/tmp/upload/" + testFilePath), testContent.getBytes("UTF-8"));
    }
    
    @After
    public void tearDown() throws IOException {
        // 清理测试文件
        Files.deleteIfExists(Paths.get(testFilePath));
    }
    
    /**
     * 功能测试：正常读取文件内容
     */
    @Test
    public void testReadFileContent_NormalFile() throws IOException {
        String content = fileService.readFileContent(testFilePath);
        assertNotNull("文件内容不应为空", content);
        assertTrue("文件内容应包含测试文本", content.contains(testContent));
    }
    
    /**
     * 功能测试：读取不存在的文件
     */
    @Test(expected = IOException.class)
    public void testReadFileContent_FileNotExists() throws IOException {
        fileService.readFileContent("nonexistent.txt");
    }
} 