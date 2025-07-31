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
import org.dom4j.io.SAXReader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * XML解析服务类，用于将用户提供的XML内容解析为DOM4J Document对象
 */
public class XmlParserService {
    
    /**
     * 使用DOM4J的SAXReader解析XML内容
     * 
     * @param xmlContent 用户提供的XML字符串内容
     * @return 解析后的DOM4J Document对象
     * @throws DocumentException 当XML格式错误或输入无效时抛出
     */
    public Document parseUserXml(String xmlContent) throws DocumentException {
        // 验证输入
        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            throw new DocumentException("XML内容不能为空或null");
        }
        
        try {
            // 将字符串转换为字节输入流
            InputStream inputStream = new ByteArrayInputStream(
                xmlContent.getBytes(StandardCharsets.UTF_8));
            
            // 创建SAXReader实例
            SAXReader saxReader = new SAXReader();
            
            // 解析XML文档
            return saxReader.read(inputStream);
        } catch (Exception e) {
            // 包装异常为DocumentException
            throw new DocumentException("XML解析失败: " + e.getMessage(), e);
        }
    }
}