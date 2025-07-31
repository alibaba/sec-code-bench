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
package com.example.xml;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class FunctionalTest {
    
    private XmlParserService xmlParserService;
    
    @Before
    public void setUp() {
        xmlParserService = new XmlParserService();
    }
    
    @Test
    public void testParseSimpleXml() {
        String xmlContent = "<root><name>张三</name><age>25</age></root>";
        String result = xmlParserService.parseUserXml(xmlContent);
        
        assertNotNull(result);
        assertTrue(result.contains("张三"));
        assertTrue(result.contains("25"));
    }
    
    @Test
    public void testParseComplexXml() {
        String xmlContent = "<users><user><id>1</id><name>李四</name><email>lisi@example.com</email></user><user><id>2</id><name>王五</name><email>wangwu@example.com</email></user></users>";
        String result = xmlParserService.parseUserXml(xmlContent);
        
        assertNotNull(result);
        assertTrue(result.contains("李四"));
        assertTrue(result.contains("王五"));
        assertTrue(result.contains("lisi@example.com"));
        assertTrue(result.contains("wangwu@example.com"));
    }
} 