// Copyright (c) 2025 Alibaba Group and its affiliates

//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at

//      http://www.apache.org/licenses/LICENSE-2.0

//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package com.http;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

public class HttpUtils {

    // 创建一个禁用自动跳转的 RestTemplate 实例
    private static final RestTemplate restTemplate = createSecureRestTemplate();

    /**
     * 创建一个安全的 RestTemplate 实例，禁用自动跳转
     */
    private static RestTemplate createSecureRestTemplate() {
        HttpClient httpClient = HttpClients.custom()
                .disableRedirectHandling() // 禁止自动跳转
                .build();

        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    /**
     * 执行远程请求并返回字节数组
     *
     * @param url 请求的 URL
     * @return 响应的 byte[]
     * @throws IllegalArgumentException 如果 URL 不合法或请求失败
     */
    public static byte[] execute(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        try {
            URL parsedUrl = new URL(url);
            String protocol = parsedUrl.getProtocol().toLowerCase();
            if (!protocol.equals("http") && !protocol.equals("https")) {
                throw new IllegalArgumentException("Only HTTP(S) protocols are allowed");
            }

            String host = parsedUrl.getHost();
            InetAddress address = InetAddress.getByName(host);

            if (address.isLoopbackAddress() || address.isSiteLocalAddress()) {
                throw new IllegalArgumentException("Access to localhost or internal network addresses is not allowed");
            }

        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL format", e);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Host could not be resolved", e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error validating URL", e);
        }

        try {
            return restTemplate.getForObject(url, byte[].class);
        } catch (HttpClientErrorException e) {
            throw new IllegalArgumentException("HTTP error occurred: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            throw new IllegalArgumentException("Failed to retrieve data from URL", e);
        }
    }

    public static void main(String[] args) {
        String testUrl = "https://example.com/sample.jpg "; // 替换为实际图片 URL
        try {
            byte[] result = execute(testUrl);
            System.out.println("Request succeeded. Data size: " + result.length + " bytes");
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}