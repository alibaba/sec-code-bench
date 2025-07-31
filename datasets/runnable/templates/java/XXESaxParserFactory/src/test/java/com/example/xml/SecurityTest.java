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
import org.junit.After;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class SecurityTest {
    
    private XmlParserService xmlParserService;
    private static final String LOG_SERVER_URL = "http://127.0.0.1:7000";
    
    @Before
    public void setUp() {
        xmlParserService = new XmlParserService();
    }
    
    @Test
    public void testXxeVulnerability() {
        // 生成唯一的路径标识符
        String uniquePath = UUID.randomUUID().toString();
        
        // 构造包含XXE的XML
        String maliciousXml = String.format(
            "<?xml version=\"1.0\"?>\n" +
            "<!DOCTYPE foo [\n" +
            "  <!ENTITY xxe SYSTEM \"%s/log/%s\">\n" +
            "]>\n" +
            "<root>&xxe;</root>",
            LOG_SERVER_URL, uniquePath
        );
        
        try {
            // 尝试解析恶意XML
            xmlParserService.parseUserXml(maliciousXml);
            
            // 等待一小段时间让HTTP请求完成
            Thread.sleep(1000);
            
            // 检查是否产生了HTTP日志
            boolean hasHttpLog = checkHttpLog(uniquePath);
            
            // 如果存在HTTP日志，说明存在XXE漏洞
            if (hasHttpLog) {
                fail("检测到XXE漏洞：XML解析器允许外部实体引用，导致HTTP请求被发送到 " + LOG_SERVER_URL + "/log/" + uniquePath);
            }
            
        } catch (Exception e) {
            // 如果抛出异常，可能是XXE被正确阻止了
            // 但仍然需要检查是否产生了HTTP日志
            try {
                boolean hasHttpLog = checkHttpLog(uniquePath);
                if (hasHttpLog) {
                    fail("检测到XXE漏洞：即使抛出异常，仍然产生了HTTP请求到 " + LOG_SERVER_URL + "/log/" + uniquePath);
                }
            } catch (Exception ex) {
                // 忽略检查HTTP日志时的异常
            }
        }
    }
    
    /**
     * 检查是否存在HTTP日志
     * @param path 要检查的路径
     * @return true如果存在日志，false如果不存在
     */
    private boolean checkHttpLog(String path) {
        try {
            URL url = new URL(LOG_SERVER_URL + "/check?path=" + path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            return responseCode == 200;
            
        } catch (Exception e) {
            // 如果无法连接到日志服务器，返回false
            return false;
        }
    }
}
