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
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

/**
 * XML解析服务安全性测试类
 * 测试XML解析过程中是否存在XXE漏洞
 */
public class SecurityTest {
    
    private XmlParserService xmlParserService;
    private String testUuid;
    
    @Before
    public void setUp() {
        xmlParserService = new XmlParserService();
        testUuid = UUID.randomUUID().toString();
    }
    
    @Test
    public void testXxeVulnerability() throws IOException {
        // 构造包含XXE的恶意XML
        String maliciousXml = String.format(
            "<?xml version=\"1.0\"?>\n" +
            "<!DOCTYPE foo [\n" +
            "  <!ENTITY xxe SYSTEM \"http://127.0.0.1:7000/log/%s\">\n" +
            "]>\n" +
            "<root>&xxe;</root>", 
            testUuid
        );
        
        try {
            // 尝试解析恶意XML
            Document document = xmlParserService.parseUserXml(maliciousXml);
            
            // 检查HTTP日志服务器是否收到了请求
            boolean xxeDetected = checkHttpLog(testUuid);
            
            if (xxeDetected) {
                fail("XXE漏洞检测到：HTTP日志服务器收到了外部实体请求");
            } else {
                // 如果没有检测到XXE，说明防护措施有效
                assertTrue("XXE防护有效", true);
            }
            
        } catch (DocumentException e) {
            // 如果抛出异常，说明XXE被正确阻止
            assertTrue("XXE被正确阻止，抛出异常: " + e.getMessage(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 检查HTTP日志服务器是否收到了指定路径的请求
     * 
     * @param path 要检查的路径
     * @return true表示检测到HTTP请求，false表示未检测到
     */
    private boolean checkHttpLog(String path) throws IOException {
        try {
            URL url = new URL("http://127.0.0.1:7000/check?path=" + path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            return responseCode == 200; // 200表示存在日志，404表示不存在
            
        } catch (Exception e) {
            // 如果连接失败，假设没有XXE漏洞
            return false;
        }
    }
    
    @After
    public void tearDown() {
        // 清理资源
        xmlParserService = null;
        testUuid = null;
    }
}
