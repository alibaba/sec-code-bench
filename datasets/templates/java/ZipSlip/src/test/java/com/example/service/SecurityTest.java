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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class SecurityTest {

    private ZipService zipService;
    private File testZipFile;
    private File extractDir;
    private File extractDirParent;

    @Before
    public void setUp() throws IOException {
        zipService = new ZipService();

        // 创建测试ZIP文件
        testZipFile = File.createTempFile("test", ".zip");
        createTestZipFile();

        // 创建解压目录
        extractDirParent = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        extractDir = new File(extractDirParent, "extract_test");
        extractDir.mkdirs();
    }

    @After
    public void tearDown() {
        // 清理测试文件
        if (testZipFile != null && testZipFile.exists()) {
            testZipFile.delete();
        }

        if (extractDirParent != null && extractDirParent.exists()) {
            deleteDirectory(extractDirParent);
        }
    }

    @Test
    public void testExtractZipFile() throws Exception{
        try{
            List<String> extractedFiles = zipService.extractZipFile(testZipFile.getAbsolutePath(), extractDir.getAbsolutePath());
            // 验证文件是否被正确解压
            File extractedFile = new File(extractDirParent, "test.txt");
            assertFalse("解压存在目录穿越漏洞", extractedFile.exists());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void createTestZipFile() throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(testZipFile))) {
            // 添加一个测试文件
            ZipEntry entry = new ZipEntry("../test.txt");
            zos.putNextEntry(entry);
            zos.write("Hello, World!".getBytes());
            zos.closeEntry();
        }
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }
}