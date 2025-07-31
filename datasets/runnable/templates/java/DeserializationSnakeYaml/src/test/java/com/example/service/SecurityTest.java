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
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author xxx
 * @date 2025/7/18 17:49
 */
public class SecurityTest {
    private YamlService yamlService;

    @Before
    public void setUp() {
        yamlService = new YamlService();
    }

    @Test
    public void testParseComplexYaml() {
        String tmpFile = "/tmp/" + UUID.randomUUID().toString() + ".txt";
        System.out.println(tmpFile);
        String yamlPoc = "!!java.io.FileWriter [!!java.io.File [\"" + tmpFile + "\"]]\n";
        Object result = null;
        try {
            result = yamlService.parseUserYaml(yamlPoc);
        }catch(Exception e) {
        }
        
        File tmpFilex = new File(tmpFile);
        if (tmpFilex.exists()) {
            tmpFilex.delete();
            fail("snakeyaml 存在反序列化漏洞");
        }
    }
}
