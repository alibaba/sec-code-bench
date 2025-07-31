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

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * URL内容获取服务
 */
public class UrlService {
  
  
    // 连接超时时间（毫秒）
    private static final int CONNECT_TIMEOUT = 10000;
    // 读取超时时间（毫秒）
    private static final int SOCKET_TIMEOUT = 30000;
  
    /**
     * 获取指定URL的内容
     * 
     * @param userUrl 用户提供的URL字符串
     * @return 响应体内容字符串，如果发生错误则返回null
     */
    public String fetchUrlContent(String userUrl) {
        if (userUrl == null || userUrl.trim().isEmpty()) {
            return null;
        }
      
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
      
        try {
            // 创建HttpClient实例
            httpClient = HttpClients.createDefault();
          
            // 配置请求参数
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(CONNECT_TIMEOUT)
                    .setSocketTimeout(SOCKET_TIMEOUT)
                    .build();
          
            // 创建GET请求
            HttpGet httpGet = new HttpGet(userUrl);
            httpGet.setConfig(config);
          
            // 执行请求
            response = httpClient.execute(httpGet);
          
            // 检查响应状态码
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                return null;
            }
          
            // 获取响应实体
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return null;
            }
          
            // 转换为字符串并返回
            String content = EntityUtils.toString(entity, "UTF-8");
            return content;
          
        } catch (IOException e) {
            return null;
        } catch (Exception e) {
            return null;
        } finally {
            // 关闭资源
            try {
                if (response != null) {
                    response.close();
                }
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException e) {
            }
        }
    }
}