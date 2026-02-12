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

package com.example.test;

import com.example.model.Profile;
import com.example.model.User;
import com.example.service.ReportService;
import org.junit.Test;

import java.io.File;
import java.util.UUID;

import static org.junit.Assert.assertFalse;

/**
 * Security tests for the ReportService.
 * This test suite includes multiple attempts to exploit a potential SSTI vulnerability
 * using various bypass techniques.
 * A test PASSES if the exploit FAILS, and FAILS if the exploit SUCCEEDS.
 */
public class SecurityTest {

    // 辅助方法，用于执行测试逻辑，避免代码重复
    private void executeTest(String payload, File maliciousFile, String testName) {
        ReportService reportService = new ReportService();
        User dummyUser = new User("test", "test@test.com", new Profile("USER", true));

        // 确保文件在测试前不存在
        if (maliciousFile.exists()) {
            maliciousFile.delete();
        }

        try {
            // 执行带有恶意Payload的服务方法
            reportService.getReportFieldValue(payload, dummyUser);
        } catch (Exception e) {
            // 如果OGNL的安全机制（如果已启用）阻止了攻击，可能会抛出异常。
            // 我们可以忽略异常，继续检查副作用。
        } finally {
            // 断言攻击失败。如果文件被创建，则测试失败，表明存在漏洞。
            assertFalse(
                "SECURITY VULNERABILITY DETECTED in " + testName + ": SSTI payload executed and created a file at " + maliciousFile.getAbsolutePath(),
                maliciousFile.exists()
            );

            // 清理：如果文件被意外创建，删除它，以免影响后续测试。
            if (maliciousFile.exists()) {
                maliciousFile.delete();
            }
        }
    }

    @Test
    public void testSstiExploitAttempt() {
        String randomFileName = "SSTI_VULN_BASIC_" + UUID.randomUUID().toString() + ".txt";
        File maliciousFile = new File(System.getProperty("java.io.tmpdir"), randomFileName);
        String filePath = maliciousFile.getAbsolutePath().replace("\\", "\\\\");

        // Payload: (new java.io.File('...')).createNewFile()
        String payload = "(new java.io.File('" + filePath + "')).createNewFile()";

        executeTest(payload, maliciousFile, "Basic Exploit");
    }

    @Test
    public void testSstiExploitAttempt2() {
        String randomFileName = "SSTI_VULN_BASIC_" + UUID.randomUUID().toString() + ".txt";
        File maliciousFile = new File(System.getProperty("java.io.tmpdir"), randomFileName);
        String filePath = maliciousFile.getAbsolutePath().replace("\\", "\\\\");

        String payload = FileUtils.createNewFilePayload(filePath);
        executeTest(payload, maliciousFile, "Basic Exploit");
    }

    @Test
    public void testSstiBypassWithUnicodeEncoding() {
        String randomFileName = "SSTI_VULN_UNICODE_" + UUID.randomUUID().toString() + ".txt";
        File maliciousFile = new File(System.getProperty("java.io.tmpdir"), randomFileName);
        String filePath = maliciousFile.getAbsolutePath().replace("\\", "\\\\");

        // Payload: (new \u006a\u0061\u0076\u0061.\u0069\u006f.\u0046\u0069\u006c\u0065('...')).createNewFile()
        String payload = "(new \u006a\u0061\u0076\u0061.\u0069\u006f.\u0046\u0069\u006c\u0065('" + filePath + "')).createNewFile()";

        executeTest(payload, maliciousFile, "Unicode Encoding Bypass");
    }

    @Test
    public void testSstiBypassWithConcatenation() {
        String randomFileName = "SSTI_VULN_CONCAT_" + UUID.randomUUID().toString() + ".txt";
        File maliciousFile = new File(System.getProperty("java.io.tmpdir"), randomFileName);
        String filePath = maliciousFile.getAbsolutePath().replace("\\", "\\\\");

        String payload = "(@java.lang.Class@forName('java.io.F' + 'ile')" +
                         ".getConstructor(@java.lang.String@class)" +
                         ".newInstance('" + filePath + "')).createNewFile()";

        executeTest(payload, maliciousFile, "String Concatenation Bypass");
    }
}

class FileUtils {
    static String createNewFilePayload(String filePath) {
        return "(new java.io.File('" + filePath + "')).createNewFile()";
    }
}
