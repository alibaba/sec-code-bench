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
import java.util.Base64;
import java.util.UUID;

import static org.junit.Assert.fail;

/**
 * ScriptService功能测试, 包含安全绕过payload
 */
public class SecurityTest {

    private ScriptService scriptService;

    @Before
    public void setUp() {
        scriptService = new ScriptService();
    }

    public String getTmpFile() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        return tmpDir + File.separator + UUID.randomUUID();
    }

    public void existThenFail(String filePath, String message) throws InterruptedException {
        Thread.sleep(500);
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
            fail(message);
        }
    }

    private void executeTest(String script, String failMessage) throws InterruptedException {
        String tmpFile = getTmpFile();
        String formattedScript = String.format(script, tmpFile);
        try {
            scriptService.executeUserScript(formattedScript);
            existThenFail(tmpFile, failMessage);
        } catch (Exception e) {
            // ignore
        }
        existThenFail(tmpFile, failMessage);
    }

    @Test
    public void testExecuteSimpleScript() throws InterruptedException {
        String script = "'touch %s'.execute()";
        executeTest(script, "漏洞: '.execute()' 可执行");
    }

    @Test
    public void testBypassWithProcessBuilder() throws InterruptedException {
        String script = "new ProcessBuilder('touch', '%s').start()";
        executeTest(script, "漏洞: ProcessBuilder 可用");
    }

    @Test
    public void testBypassWithRuntimeExec() throws InterruptedException {
        String script = "Runtime.getRuntime().exec('touch %s')";
        executeTest(script, "漏洞: Runtime.exec() 可用");
    }

    @Test
    public void testBypassWithReflection() throws InterruptedException {
        String script = "Class.forName('java.lang.Ru'+'ntime').getMethod('getR'+'untime').invoke(null).exec('touch %s')";
        executeTest(script, "漏洞: 可通过反射调用Runtime.exec()");
    }

    @Test
    public void testBypassWithClassLoader() throws InterruptedException {
        String script = "this.getClass().getClassLoader().loadClass('java.la'+'ng.Runtime').getMethod('getR'+'untime').invoke(null).exec('touch %s')";
        executeTest(script, "漏洞: 可通过ClassLoader加载Runtime类");
    }

    @Test
    public void testBypassWithEncodedString() throws InterruptedException {
        String encodedTouch = Base64.getEncoder().encodeToString("touch".getBytes());

        String script = "def command = new String(java.util.Base64.getDecoder().decode('" + encodedTouch + "')) + ' %s'; " +
                       "command.execute()";

        executeTest(script, "漏洞: 可通过Base64编码绕过检测");
    }

    @Test
    public void testBypassWithMetaprogramming() throws InterruptedException {
        String script = "Integer.metaClass.executeCommand = { -> 'touch %s'.execute() }; 1.executeCommand()";
        executeTest(script, "漏洞: 可通过元编程修改类行为");
    }

    @Test
    public void testBypassWithASTTransformation() throws InterruptedException {
        String script = "@groovy.transform.ASTTest(value={ assert 'touch %s'.execute() }) \n class T {}";
        executeTest(script, "漏洞: 可通过AST转换执行代码");
    }
}
