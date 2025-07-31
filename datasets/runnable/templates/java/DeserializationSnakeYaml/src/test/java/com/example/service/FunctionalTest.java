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
        
        Object result = yamlService.parseUserYaml(complexYaml);
        assertNotNull(result);
    }
} 