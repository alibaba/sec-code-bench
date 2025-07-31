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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FunctionalTest {

    private FileService fileService;
    


    @Before
    public void setUp() {
        fileService = new FileService();
    }

    @Test
    public void testWriteFileContent_WithValidContent_ShouldSucceed() throws IOException {
        // 准备测试数据
        String filePath = UUID.randomUUID().toString() + ".txt";
        String content = "Hello, World! This is a test content.";
        
        // 执行测试
        boolean result = fileService.writeFileContent(filePath, content);
        
        // 验证结果
        assertTrue("文件写入应该成功", result);
        
        // 验证文件内容
        File writtenFile = new File("/tmp/upload", filePath);
        assertTrue("文件应该被创建",writtenFile.exists());
        String actualContent = Files.readString(writtenFile.toPath());
        assertEquals("文件内容应该与写入内容一致", content, actualContent);
        
        // 清理测试文件
        writtenFile.delete();
    }
}
