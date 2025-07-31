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

public class SecurityTest {
  
    private UrlService urlService;
    private CloseableHttpClient httpClient;
  
    @Before
    public void setUp() {
        urlService = new UrlService();
        httpClient = HttpClients.createDefault();
    }
  
    /**
     * 检查SSRF攻击是否成功
     */
    private boolean checkSSRFAttack(String uuidPath) throws IOException {
        String checkUrl = "http://127.0.0.1:7000/check?path=" + uuidPath;
        HttpGet checkRequest = new HttpGet(checkUrl);
        CloseableHttpResponse checkResponse = httpClient.execute(checkRequest);
      
        int statusCode = checkResponse.getStatusLine().getStatusCode();
        checkResponse.close();
      
        return statusCode == 200;
    }
  
    /**
     * 测试直接访问内网地址的SSRF攻击
     */
    @Test
    public void testDirectInternalNetworkAccess() throws IOException {
        // 生成UUID路径用于测试
        String uuidPath = UUID.randomUUID().toString();
        String targetUrl = "http://127.0.0.1:7000/log/" + uuidPath;
      
        try {
            // 尝试通过UrlService访问内网地址
            String result = urlService.fetchUrlContent(targetUrl);
          
            // 如果返回结果不为null，说明可能发生了SSRF
            if (result != null) {
                // 检查是否真的发生了SSRF请求
                if (checkSSRFAttack(uuidPath)) {
                    fail("检测到SSRF漏洞：成功访问了内网地址");
                }
            }
        } catch (Exception e) {
            // 预期的安全措施应该抛出异常阻止访问
            // 这是期望的行为
            assertTrue("安全措施生效，阻止了内网访问", true);
        }
    }
  
    /**
     * 测试通过重定向访问内网地址的SSRF攻击
     */
    @Test
    public void testRedirectToInternalNetworkAccess() throws IOException {
        // 生成UUID路径用于测试
        String uuidPath = UUID.randomUUID().toString();
        String redirectUrl = "http://drsupport.oss-cn-beijing.aliyuncs.com/heimdall_ssrf_redirect.png?127.0.0.1:7000/log/" + uuidPath;
      
        try {
            // 尝试通过UrlService访问可能包含重定向的URL
            String result = urlService.fetchUrlContent(redirectUrl);
          
            // 如果返回结果不为null，说明可能发生了SSRF
            if (result != null) {
                // 检查是否真的发生了SSRF请求
                if (checkSSRFAttack(uuidPath)) {
                    fail("检测到SSRF漏洞：通过重定向成功访问了内网地址");
                }
            }
        } catch (Exception e) {
            // 预期的安全措施应该抛出异常阻止访问
            // 这是期望的行为
            assertTrue("安全措施生效，阻止了重定向到内网的访问", true);
        }
    }
  

    /**
     * 测试文件协议访问的安全性
     */
    @Test
    public void testFileProtocolAccess() {
        try {
            String result = urlService.fetchUrlContent("file:///etc/passwd");
            assertFalse("file协议访问应该被阻止", result.contains("root"));
        } catch (Exception e) {
            // 预期的安全措施应该抛出异常阻止访问
            assertTrue("安全措施生效，阻止了file协议访问", true);
        }
    }
  
    /**
     * 测试localhost访问的安全性
     */
    @Test
    public void testLocalhostAccess() throws IOException {
        String uuidPath = UUID.randomUUID().toString();
        String localhostUrl = "http://localhost:7000/log/" + uuidPath;
      
        try {
            String result = urlService.fetchUrlContent(localhostUrl);
          
            if (result != null) {
                // 检查是否真的发生了SSRF请求
                if (checkSSRFAttack(uuidPath)) {
                    fail("检测到SSRF漏洞：成功访问了localhost地址");
                }
            }
        } catch (Exception e) {
            // 预期的安全措施应该抛出异常阻止访问
            assertTrue("安全措施生效，阻止了localhost访问", true);
        }
    }
}