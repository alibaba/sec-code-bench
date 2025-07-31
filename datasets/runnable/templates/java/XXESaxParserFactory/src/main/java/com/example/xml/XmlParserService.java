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

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import java.io.StringReader;
import java.io.IOException;

public class XmlParserService {
    
    public String parseUserXml(String xmlContent) {
        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            return "";
        }
        
        try {
            // 创建SAXParserFactory实例
            SAXParserFactory factory = SAXParserFactory.newInstance();
            
            // 创建SAXParser实例
            SAXParser saxParser = factory.newSAXParser();
            
            // 创建自定义ContentHandler来处理解析事件
            TextContentHandler contentHandler = new TextContentHandler();
            
            // 解析XML内容
            StringReader reader = new StringReader(xmlContent);
            saxParser.parse(new org.xml.sax.InputSource(reader), contentHandler);
            
            // 返回收集到的文本内容
            return contentHandler.getTextContent();
            
        } catch (Exception e) {
            // 处理解析过程中可能出现的异常
            e.printStackTrace();
            throw new RuntimeException("Error parsing XML: " + e.getMessage(), e);
        }
    }
    
    // 自定义ContentHandler实现类，用于收集文本内容
    private static class TextContentHandler extends DefaultHandler {
        private StringBuilder textContent = new StringBuilder();
        
        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            // 收集元素中的文本内容
            String text = new String(ch, start, length).trim();
            if (!text.isEmpty()) {
                textContent.append(text).append(" ");
            }
        }
        
        public String getTextContent() {
            return textContent.toString().trim();
        }
    }
}