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

package com.example;

import com.example.service.DynamicMetricService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.UUID;

/**
 * 针对 DynamicMetricService 的安全测试套件。
 */
public class SecurityTest {

    private DynamicMetricService metricService;
    private Path rceTestFilePath;

    @Before
    public void setUp() {
        metricService = new DynamicMetricService();
        // @Before 中不创建文件，让每个测试自己创建，以保持独立性
        rceTestFilePath = null;
    }

    @After
    public void tearDown() throws IOException {
        // 清理在测试中可能创建的文件
        if (rceTestFilePath != null) {
            Files.deleteIfExists(rceTestFilePath);
        }
    }

    /**
     * 创建一个唯一的临时文件路径并存储在成员变量中。
     * @return Path 临时文件路径
     */
    private Path createAndSetTempFilePath() {
        String tempDir = System.getProperty("java.io.tmpdir");
        String uniqueFileName = "rce_test_" + UUID.randomUUID().toString() + ".txt";
        this.rceTestFilePath = Paths.get(tempDir, uniqueFileName);
        return this.rceTestFilePath;
    }

    /**
     * 执行 payload 并检查攻击是否成功（文件是否被创建）。
     * @param payload 恶意脚本
     * @return boolean 如果文件被创建，则返回 true，表示攻击成功
     */
    private boolean executePayloadAndCheck(String payload) {
        try {
            metricService.calculate(payload, Collections.emptyMap());
        } catch (Exception e) {
            // 在安全的实现中，这里可能会抛出 SecurityException。
            // 我们捕获所有异常，因为最终的判断标准是文件是否被创建。
        }
        return Files.exists(this.rceTestFilePath);
    }

    @Test
    public void testRceAttackAttempt_With_SimpleFileCreation() {
        createAndSetTempFilePath();
        String filePathString = this.rceTestFilePath.toAbsolutePath().toString().replace("\\", "\\\\");
        String payload = String.format("new java.io.FileWriter('%s').close()", filePathString);

        boolean attackSucceeded = executePayloadAndCheck(payload);
        Assert.assertFalse("安全漏洞：基础的文件创建攻击成功！", attackSucceeded);
    }

    @Test
    public void testRceAttackAttempt_With_CommandExecution() {
        createAndSetTempFilePath();
        String filePathString = this.rceTestFilePath.toAbsolutePath().toString();
        String os = System.getProperty("os.name").toLowerCase();
        String payload;
        if (os.contains("win")) {
            payload = String.format("new java.lang.ProcessBuilder('cmd', '/c', 'echo > %s').start().waitFor()", filePathString.replace("\\", "\\\\"));
        } else {
            payload = String.format("new java.lang.ProcessBuilder('touch', '%s').start().waitFor()", filePathString);
        }

        boolean attackSucceeded = executePayloadAndCheck(payload);
        Assert.assertFalse("安全漏洞：通过ProcessBuilder执行命令成功！", attackSucceeded);
    }

    @Test
    public void testRceAttackAttempt_With_ReflectionBypass() {
        createAndSetTempFilePath();
        String filePathString = this.rceTestFilePath.toAbsolutePath().toString().replace("\\", "\\\\");
        String payload = String.format(
            "var FileWriter = java.lang.Class.forName('java.io.Fi'+'leWriter');" +
            "var constructor = FileWriter.getConstructor(java.lang.String.class);" +
            "var writer = constructor.newInstance('%s');" +
            "writer.close();",
            filePathString
        );

        boolean attackSucceeded = executePayloadAndCheck(payload);
        Assert.assertFalse("安全漏洞：通过反射API绕过成功！", attackSucceeded);
    }

    @Test
    public void testRceAttackAttempt_With_JavaCommentBypass() {
        createAndSetTempFilePath();
        String payload = String.format(
            "function test(){ return java.io./****/File };" +
            "var f = test();" +
            "new f('%s').createNewFile()",
            this.rceTestFilePath.toAbsolutePath().toString().replace("\\", "\\\\")
        );

        boolean attackSucceeded = executePayloadAndCheck(payload);
        Assert.assertFalse("安全漏洞：通过'注释混淆'绕过成功！", attackSucceeded);
    }

    @Test
    public void testRceAttackAttempt_With_EvalAndFromCharCodeBypass() {
        createAndSetTempFilePath();
        String clearTextPayload = String.format("new java.io.File('%s').createNewFile()",
            this.rceTestFilePath.toAbsolutePath().toString().replace("\\", "\\\\"));

        StringBuilder charCodes = new StringBuilder();
        for (int i = 0; i < clearTextPayload.length(); i++) {
            charCodes.append((int) clearTextPayload.charAt(i));
            if (i < clearTextPayload.length() - 1) {
                charCodes.append(",");
            }
        }

        String payload = String.format(
            "eval(String.fromCharCode(%s))",
            charCodes.toString()
        );

        boolean attackSucceeded = executePayloadAndCheck(payload);
        Assert.assertFalse("安全漏洞：通过'eval+fromCharCode'绕过成功！", attackSucceeded);
    }
}