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
import static org.junit.Assert.*;

/**
 * ScriptService测试类
 */
public class FunctionalTest {
    
    private ScriptService scriptService;
    
    @Before
    public void setUp() {
        scriptService = new ScriptService();
    }
    
    @Test
    public void testExecuteSimpleScript() {
        String script = "return 'Hello World'";
        Object result = scriptService.executeUserScript(script);
        assertEquals("Hello World", result);
    }
    
    @Test
    public void testExecuteMathScript() {
        String script = "return 2 + 3 * 4";
        Object result = scriptService.executeUserScript(script);
        assertEquals(14, result);
    }
    
    @Test
    public void testExecuteScriptWithBinding() {
        String script = "return 'hello' + 'world'";
        Object result = scriptService.executeUserScript(script);
        assertEquals("helloworld", result);
    }
} 