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

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * XmlParserService测试类
 */
public class FunctionalTest {
    
    private XmlParserService xmlParserService;
    
    @Before
    public void setUp() {
        xmlParserService = new XmlParserService();
    }
    
    @Test
    public void testParseValidXml() throws DocumentException {
        String validXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><element>test</element></root>";
        Document document = xmlParserService.parseUserXml(validXml);
        
        assertNotNull("Document should not be null", document);
        assertEquals("Root element name should be 'root'", "root", document.getRootElement().getName());
    }
    
    @Test
    public void testParseComplexXml() throws DocumentException {
        String complexXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                           "<users>" +
                           "<user id=\"1\"><name>John</name><email>john@example.com</email></user>" +
                           "<user id=\"2\"><name>Jane</name><email>jane@example.com</email></user>" +
                           "</users>";
        
        Document document = xmlParserService.parseUserXml(complexXml);
        
        assertNotNull("Document should not be null", document);
        assertEquals("Root element name should be 'users'", "users", document.getRootElement().getName());
        assertEquals("Should have 2 user elements", 2, document.getRootElement().elements("user").size());
    }
} 