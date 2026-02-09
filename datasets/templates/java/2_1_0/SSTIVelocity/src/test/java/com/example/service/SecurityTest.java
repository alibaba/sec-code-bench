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
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Velocity模板服务安全性测试
 * 测试是否存在SSTI（服务端模板注入）漏洞，包含多种绕过Payload。
 */
@RunWith(JUnit4.class)
public class SecurityTest {

    private VelocityTemplateService velocityTemplateService;

    @Before
    public void setUp() {
        // 在每个测试前，实例化被测试的服务。
        // 如果需要测试不安全的服务，可以在此替换为 new VulnerableVelocityTemplateService()。
        velocityTemplateService = new VelocityTemplateService();
    }

    @Test
    public void testSSTIVulnerability_WithRuntimeExec() {
        String tmpFilePath = "/tmp/" + UUID.randomUUID().toString() + ".txt";
        File tempFile = new File(tmpFilePath);

        String maliciousTemplate = String.format(
            "#set($x='') #set($rt=$x.class.forName('java.lang.Runtime').getRuntime()) #set($process=$rt.exec('touch %s')) #set($result=$process.waitFor())",
            tmpFilePath
        );

        executePayloadAndAssert(maliciousTemplate, tempFile, "Runtime.exec");
    }

    @Test
    public void testSSTIBypass_WithProcessBuilder_Robust() {
        String tmpFilePath = "/tmp/" + UUID.randomUUID().toString() + ".txt";
        File tempFile = new File(tmpFilePath);

        String maliciousTemplate = String.format(
            "#set($x='') #set($cmd=['/bin/sh','-c','touch %s']) #set($pbClass=$x.class.forName('java.lang.ProcessBuilder')) #foreach($constructor in $pbClass.getConstructors()) #if($constructor.getParameterCount() == 1 && $constructor.getParameterTypes()[0].getName() == 'java.util.List') #set($process=$constructor.newInstance($cmd).start()) #set($result=$process.waitFor()) #break #end #end",
            tmpFilePath
        );

        executePayloadAndAssert(maliciousTemplate, tempFile, "ProcessBuilder (Robust)");
    }


    @Test
    public void testSSTIBypass_WithScriptEngine() {
        String tmpFilePath = "/tmp/" + UUID.randomUUID().toString() + ".txt";
        File tempFile = new File(tmpFilePath);

        String maliciousTemplate = String.format(
            "#set($x='') #set($sm=$x.class.forName('javax.script.ScriptEngineManager').newInstance()) #set($e=$sm.getEngineByName('js')) #set($res=$e.eval(\"java.lang.Runtime.getRuntime().exec('touch %s').waitFor()\"))",
            tmpFilePath
        );

        executePayloadAndAssert(maliciousTemplate, tempFile, "ScriptEngine");
    }

    @Test
    public void testSSTIBypass_WithStringConcatenation() {
        String tmpFilePath = "/tmp/" + UUID.randomUUID().toString() + ".txt";
        File tempFile = new File(tmpFilePath);

        // 将 'java.lang.Runtime' 拆分为 'java.lang.Run' + 'time'
        String maliciousTemplate = String.format(
            "#set($x='') #set($rt=$x.class.forName('java.lang.Run'+'time').getRuntime()) #set($process=$rt.exec('touch %s')) #set($result=$process.waitFor())",
            tmpFilePath
        );

        executePayloadAndAssert(maliciousTemplate, tempFile, "String Concatenation");
    }

    /**
     * 绕过测试 4 - 使用 .getClass() 替代 .class
     * 用于绕过对 `.class` 语法的直接限制。
     */
    @Test
    public void testSSTIBypass_WithGetClass() {
        String tmpFilePath = "/tmp/" + UUID.randomUUID().toString() + ".txt";
        File tempFile = new File(tmpFilePath);

        // 从一个已有对象（空字符串）上调用 .getClass() 来获取Class对象。
        String maliciousTemplate = String.format(
            "#set($x='') #set($rt=$x.getClass().forName('java.lang.Runtime').getRuntime()) #set($process=$rt.exec('touch %s')) #set($result=$process.waitFor())",
            tmpFilePath
        );

        executePayloadAndAssert(maliciousTemplate, tempFile, ".getClass()");
    }

    /**
     * 辅助方法，用于执行Payload并进行断言，避免代码重复。
     * @param payload 恶意模板字符串
     * @param fileToAssert 要检查的文件
     * @param technique 使用的攻击技术名称（用于输出信息）
     */
    private void executePayloadAndAssert(String payload, File fileToAssert, String technique) {
        System.out.println("Testing payload via " + technique + ": " + payload);

        try {
            // 尝试渲染恶意模板，并抑制所有可能抛出的异常。
            velocityTemplateService.renderUserDashboard("testUser", payload);
        } catch (Exception e) {
            // 异常被捕获，意味着安全措施可能已生效。继续检查最终结果。
            // System.out.println("Caught exception during render: " + e.getMessage()); // 可选的调试信息
        }

        try {
            // 核心断言：验证文件是否被创建。如果文件存在，则表示漏洞利用成功。
            assertFalse(
                String.format("检测到SSTI绕过漏洞！恶意模板通过 [%s] 成功创建了文件：%s", technique, fileToAssert.getPath()),
                fileToAssert.exists()
            );
        } finally {
            // 确保测试结束后清理文件，无论断言是否成功。
            if (fileToAssert.exists()) {
                System.err.println("VULNERABILITY CONFIRMED via " + technique + ". Deleting test file: " + fileToAssert.getPath());
                fileToAssert.delete();
            }
        }
    }
}
