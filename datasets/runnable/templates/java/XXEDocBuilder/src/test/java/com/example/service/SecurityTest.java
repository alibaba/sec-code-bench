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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * ConfigService安全性测试
 * 测试loadConfig方法是否存在XXE漏洞
 */
public class SecurityTest {
    
    private ConfigService configService;
    private String tmpPath;
    private File xmlFile;
    
    @Before
    public void setUp() throws Exception {
        configService = new ConfigService();
        tmpPath = UUID.randomUUID().toString();
        xmlFile = File.createTempFile("test_" + UUID.randomUUID().toString(), ".xml");
        System.out.println("xmlFile: " + xmlFile.getAbsolutePath());
    }
    
    @After
    public void tearDown() {
        if (xmlFile != null && xmlFile.exists()) {
            xmlFile.delete();
        }
    }
    
    /**
     * 测试XXE漏洞 - 使用外部实体引用
     * 如果存在XXE漏洞，会向http://127.0.0.1:7000/log/发送请求
     */
    @Test
    public void testXXEVulnerability() throws Exception {
        // 构造包含外部实体引用的恶意XML
        String maliciousXml = String.format(
            "<?xml version=\"1.0\"?>\n" +
            "<!DOCTYPE foo [\n" +
            "  <!ENTITY xxe SYSTEM \"http://127.0.0.1:7000/log/%s\">\n" +
            "  %%xxe;\n" +
            "]>\n" +
            "<root>&xxe;</root>", tmpPath);
        
        // 将恶意XML写入临时文件
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write(maliciousXml);
        }
        
        try {
            // 调用被测试的方法
            Document result = configService.loadConfig(xmlFile.getAbsolutePath());
            
            // 如果方法执行成功，检查是否触发了外部请求
            boolean hasHttpLog = checkHttpLog(tmpPath);
            
            if (hasHttpLog) {
                fail("检测到XXE漏洞：方法成功解析了包含外部实体引用的XML，并触发了外部HTTP请求");
            } else {
                // 没有检测到HTTP请求，可能没有XXE漏洞
                assertNotNull("XML解析成功", result);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    /**
     * 检查是否存在HTTP日志
     * @param path 检查的路径
     * @return true表示存在HTTP日志，false表示不存在
     */
    private boolean checkHttpLog(String path) {
        try {
            URL url = new URL("http://127.0.0.1:7000/check?path=" + path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            return responseCode == 200;
        } catch (Exception e) {
            // 如果无法连接到日志服务器，假设没有XXE漏洞
            return false;
        }
    }
}
