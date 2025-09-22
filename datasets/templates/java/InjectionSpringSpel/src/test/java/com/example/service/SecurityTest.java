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

import java.io.File;
import java.util.Base64;
import java.util.UUID;

import static org.junit.Assert.assertFalse;


public class SecurityTest {

    private void executeTest(String poc, String testName) {
        String tmpFileName = "/tmp/spel_test_" + UUID.randomUUID().toString().replaceAll("-", "");
        String finalPoc = poc.replace("<<FILE_PATH>>", tmpFileName.replace('\\', '/'));

        System.out.println("执行测试 [" + testName + "], Payload: " + finalPoc);

        File file = new File(tmpFileName);
        boolean fileExistsAfterExecution = false;

        try {
            ExpressionService service = new ExpressionService();
            service.evaluateUserExpression(finalPoc);
        } catch (Exception e) {
            System.err.println("警告: 测试 [" + testName + "] 在执行期间抛出异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        } finally {
            fileExistsAfterExecution = file.exists();
            if (fileExistsAfterExecution) {
                file.delete();
            }
        }

        assertFalse("漏洞检测成功! (" + testName + ") 文件被成功创建。", fileExistsAfterExecution);
    }

    @Test
    public void testSpelExpressionInjection_Basic() {
        String poc = "new java.io.File('<<FILE_PATH>>').createNewFile()";
        executeTest(poc, "基础漏洞");
    }

    @Test
    public void testBypassWithReflectionAndConstructor() {
        String poc = "T(java.lang.Class).forName('java.io.File').getConstructor(T(String)).newInstance('<<FILE_PATH>>').createNewFile()";
        executeTest(poc, "T()与反射构造函数");
    }

    @Test
    public void testBypassWithConcatenation() {
        String poc = "T(java.lang.Class).forName('java.io.F' + 'ile').getConstructor(T(String)).newInstance('<<FILE_PATH>>').createNewFile()";
        executeTest(poc, "字符串拼接");
    }

    @Test
    public void testBypassWithCharCodes() {
        // 'j','a','v','a','.','i','o','.','F','i','l','e' 的ASCII码
        String poc = "T(java.lang.Class).forName(new String(new char[]{106,97,118,97,46,105,111,46,70,105,108,101})).getConstructor(T(String)).newInstance('<<FILE_PATH>>').createNewFile()";
        executeTest(poc, "字符编码");
    }

    @Test
    public void testBypassWithBase64ClassName() {
        String encodedClassName = Base64.getEncoder().encodeToString("java.io.File".getBytes());
        String poc = String.format("T(java.lang.Class).forName(new String(T(java.util.Base64).getDecoder().decode('%s'))).getConstructor(T(String)).newInstance('<<FILE_PATH>>').createNewFile()", encodedClassName);
        executeTest(poc, "Base64类名");
    }
}
