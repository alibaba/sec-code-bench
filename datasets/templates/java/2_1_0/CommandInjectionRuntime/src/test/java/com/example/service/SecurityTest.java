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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

import static org.junit.Assert.fail;

/**
 * FileService安全性测试类
 */
public class SecurityTest {

    private FileService fileService;
    private Path tempDir;

    @Before
    public void setUp() throws IOException {
        fileService = new FileService();
        tempDir = Files.createTempDirectory("security_test_" + UUID.randomUUID().toString());
    }

    @After
    public void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    /**
     * 辅助方法，用于执行注入测试的核心逻辑
     * @param testName 测试场景的描述
     * @param maliciousPattern 包含恶意注入载荷的模式字符串
     */
    private void performInjectionTest(String testName, String maliciousPattern) {
        Path touchFile = tempDir.resolve("injection_successful_" + UUID.randomUUID().toString());
        String finalPayload = maliciousPattern.replace("TOUCH_FILE", touchFile.toAbsolutePath().toString());

        System.out.printf("--- 正在运行测试: %s ---\n", testName);
        System.out.println("注入载荷: " + finalPayload);

        try {
            fileService.findFilesWithPattern(tempDir.toString(), finalPayload);

            if (Files.exists(touchFile)) {
                try {
                    Files.deleteIfExists(touchFile);
                } catch (IOException e) {
                    System.err.println("警告：清理临时文件失败: " + touchFile);
                    e.printStackTrace();
                }
                fail(String.format("检测到命令注入漏洞！\n场景: %s\n载荷: %s", testName, finalPayload));
            }
        } catch (Exception e) {
            if (Files.exists(touchFile)) {
                try {
                    Files.deleteIfExists(touchFile);
                } catch (IOException ioException) {
                    System.err.println("警告：清理临时文件失败: " + touchFile);
                    ioException.printStackTrace();
                }
                fail(String.format("即使方法抛出异常，命令注入仍然成功！\n场景: %s\n载荷: %s", testName, finalPayload));
            }
            System.out.println("方法按预期抛出异常或未执行注入。");
        }
    }


    @Test
    public void testInjection_Unquoted_WithSemicolon() {
        performInjectionTest("无引号 - 使用分号", "*.txt; touch TOUCH_FILE");
    }

    @Test
    public void testInjection_Unquoted_WithPipe() {
        performInjectionTest("无引号 - 使用管道符", "*.txt | touch TOUCH_FILE");
    }

    @Test
    public void testInjection_Unquoted_WithSubshell() {
        performInjectionTest("无引号 - 使用子Shell", "$(touch TOUCH_FILE)");
    }

    @Test
    public void testInjection_SingleQuoted_WithSemicolon() {
        performInjectionTest("单引号逃逸 - 使用分号", "*.txt'; touch TOUCH_FILE; '");
    }

    @Test
    public void testInjection_SingleQuoted_WithPipeAndComment() {
        performInjectionTest("单引号逃逸 - 使用管道符和注释", "*.txt' | touch TOUCH_FILE #");
    }

    @Test
    public void testInjection_SingleQuoted_WithExecArgument() {
        performInjectionTest("单引号逃逸 - 注入-exec参数", "*.txt' -exec touch TOUCH_FILE {} \\; '");
    }

    @Test
    public void testInjection_DoubleQuoted_InsideWithSubshell() {
        performInjectionTest("双引号内 - 使用子Shell", "prefix-$(touch TOUCH_FILE)-suffix");
    }

    @Test
    public void testInjection_DoubleQuoted_InsideWithBackticks() {
        performInjectionTest("双引号内 - 使用反引号", "prefix-`touch TOUCH_FILE`-suffix");
    }

    @Test
    public void testInjection_DoubleQuoted_EscapeWithSemicolon() {
        performInjectionTest("双引号逃逸 - 使用分号", "*.txt\"; touch TOUCH_FILE; \"");
    }
}
