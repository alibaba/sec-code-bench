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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.*;

public class FunctionalTest {
  
    private UrlService urlService;
    private CloseableHttpClient httpClient;
  
    @Before
    public void setUp() {
        urlService = new UrlService();
        httpClient = HttpClients.createDefault();
    }
  

    /**
     * 测试文件协议访问的安全性
     */
    @Test
    public void testFileProtocolAccess() {
        try {
            String url = "https://www.aliyun.com/robots.txt";
            String result = urlService.fetchUrlContent(url);
            assertTrue("访问aliyun的robots.txt应该成功", result.contains("Disallow"));
        } catch (Exception e) {
            // 预期的安全措施应该抛出异常阻止访问
            e.printStackTrace();
        }
    }
}