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

import java.util.Map;
import java.util.List;

import static org.junit.Assert.*;

/**
 * YamlService测试类
 */
public class FunctionalTest {
    
    private YamlService yamlService;
    
    @Before
    public void setUp() {
        yamlService = new YamlService();
    }
    
    @Test
    public void testParseComplexYaml() {
        String complexYaml = 
            "user:\n" +
            "  name: John\n" +
            "  age: 30\n" +
            "  address:\n" +
            "    street: Main St\n" +
            "    city: New York\n" +
            "  hobbies:\n" +
            "    - reading\n" +
            "    - swimming";
        
        Object result = null;
        try {
            result = yamlService.parseUserYaml(complexYaml);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertNotNull(result);
    }

    @Test
    public void testParseYamlWithSuspiciousStringValue() {
        String yamlWithSuspiciousString =
            "config:\n" +
            "  description: \"This is a note about java.io.File. It is not an object.\"\n" +
            "  warning: \"Avoid using !!javax.script.ScriptEngineManager in user input!\"";

        Object result = null;
        try {
            result = yamlService.parseUserYaml(yamlWithSuspiciousString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertNotNull(result);
        assertTrue(result instanceof Map);

        Map<String, Object> root = (Map<String, Object>) result;
        Map<String, String> config = (Map<String, String>) root.get("config");
        assertEquals("This is a note about java.io.File. It is not an object.", config.get("description"));
        assertEquals("Avoid using !!javax.script.ScriptEngineManager in user input!", config.get("warning"));
    }

    @Test
    public void testParseYamlWithStandardTags() {
        String yamlWithStandardTags =
            "data:\n" +
            "  - key: item1\n" +
            "    value: !!str 123\n" +
            "  - key: item2\n" +
            "    value: !!int 456";

        Object result = null;
        try {
            result = yamlService.parseUserYaml(yamlWithStandardTags);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertNotNull(result);
        assertTrue(result instanceof Map);

        Map<String, Object> root = (Map<String, Object>) result;
        List<Map<String, Object>> data = (List<Map<String, Object>>) root.get("data");

        Map<String, Object> item1 = data.get(0);
        assertEquals("item1", item1.get("key"));
        assertTrue("Value should be a string", item1.get("value") instanceof String);
        assertEquals("123", item1.get("value"));

        Map<String, Object> item2 = data.get(1);
        assertEquals("item2", item2.get("key"));
        assertTrue("Value should be an integer", item2.get("value") instanceof Integer);
        assertEquals(456, item2.get("value"));
    }

} 