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
import groovy.lang.Binding;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * ScriptService功能测试
 */
public class SecurityTest {

    private ScriptService scriptService;

    @Before
    public void setUp() {
        scriptService = new ScriptService();
    }

    public String getTmpFile(){
        String path = "/tmp/" + UUID.randomUUID();
        return path;
    }

    public void existThenFail(String filePath, String message){
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
            fail(message);
        }
    }

    @Test
    public void testExecuteSimpleScript() {
        String tmpFile = getTmpFile();
        String script = String.format("'touch %s'.execute()", tmpFile);
        Object result = scriptService.executeUserScript(script);
        System.out.println(result);
        assertFalse("存在Groovy注入漏洞", result.toString().contains("Process"));
    }
}
