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
import org.junit.After;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * ConfigService的单元测试类
 */
public class FunctionalTest {

    private ConfigService configService;
    private File tempConfigFile;
    private File tempInvalidFile;

    @Before
    public void setUp() throws Exception {
        configService = new ConfigService();
        
        // 创建临时的有效XML配置文件
        tempConfigFile = File.createTempFile("test_config", ".xml");
        try (FileWriter writer = new FileWriter(tempConfigFile)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<config>\n");
            writer.write("    <database>\n");
            writer.write("        <host>localhost</host>\n");
            writer.write("        <port>3306</port>\n");
            writer.write("        <username>admin</username>\n");
            writer.write("        <password>secret</password>\n");
            writer.write("    </database>\n");
            writer.write("    <logging>\n");
            writer.write("        <level>INFO</level>\n");
            writer.write("        <file>/var/log/app.log</file>\n");
            writer.write("    </logging>\n");
            writer.write("</config>");
        }

        // 创建临时的无效XML文件
        tempInvalidFile = File.createTempFile("test_invalid", ".xml");
        try (FileWriter writer = new FileWriter(tempInvalidFile)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<config>\n");
            writer.write("    <database>\n");
            writer.write("        <host>localhost</host>\n");
            writer.write("        <!-- 缺少闭合标签，导致XML格式错误 -->\n");
            writer.write("    </database>");
            // 故意不闭合config标签
        }
    }

    @After
    public void tearDown() {
        // 清理临时文件
        if (tempConfigFile != null && tempConfigFile.exists()) {
            tempConfigFile.delete();
        }
        if (tempInvalidFile != null && tempInvalidFile.exists()) {
            tempInvalidFile.delete();
        }
    }

    @Test
    public void testLoadValidConfig() throws Exception {
        // 测试加载有效的XML配置文件
        Document document = configService.loadConfig(tempConfigFile.getAbsolutePath());

        assertNotNull("解析的文档不应为空", document);
        
        Element root = document.getDocumentElement();
        assertEquals("根元素名称应为config", "config", root.getNodeName());

        // 验证数据库配置
        NodeList databaseNodes = document.getElementsByTagName("database");
        assertEquals("应该有一个database节点", 1, databaseNodes.getLength());

        NodeList hostNodes = document.getElementsByTagName("host");
        assertEquals("应该有一个host节点", 1, hostNodes.getLength());
        assertEquals("host值应为localhost", "localhost", hostNodes.item(0).getTextContent());

        NodeList portNodes = document.getElementsByTagName("port");
        assertEquals("应该有一个port节点", 1, portNodes.getLength());
        assertEquals("port值应为3306", "3306", portNodes.item(0).getTextContent());

        // 验证日志配置
        NodeList loggingNodes = document.getElementsByTagName("logging");
        assertEquals("应该有一个logging节点", 1, loggingNodes.getLength());

        NodeList levelNodes = document.getElementsByTagName("level");
        assertEquals("应该有一个level节点", 1, levelNodes.getLength());
        assertEquals("level值应为INFO", "INFO", levelNodes.item(0).getTextContent());
    }
} 