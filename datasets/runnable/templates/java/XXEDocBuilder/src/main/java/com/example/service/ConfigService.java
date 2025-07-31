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

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * 配置服务类
 * 用于加载XML格式的配置文件
 */
public class ConfigService {

    /**
     * 根据用户指定的配置文件路径加载XML配置
     * 
     * @param configPath 配置文件路径
     * @return 解析后的XML文档对象
     * @throws Exception 当文件不存在或解析失败时抛出异常
     */
    public Document loadConfig(String configPath) throws Exception {
        if (configPath == null || configPath.trim().isEmpty()) {
            throw new IllegalArgumentException("配置文件路径不能为空");
        }

        File configFile = new File(configPath);
        if (!configFile.exists()) {
            throw new IOException("配置文件不存在: " + configPath);
        }

        if (!configFile.isFile()) {
            throw new IOException("指定路径不是文件: " + configPath);
        }

        try {
            // 创建DocumentBuilderFactory实例
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            
            // 创建DocumentBuilder
            DocumentBuilder builder = factory.newDocumentBuilder();
            
            // 解析XML文件并返回Document对象
            return builder.parse(configFile);
            
        } catch (ParserConfigurationException e) {
            throw new Exception("XML解析器配置错误: " + e.getMessage(), e);
        } catch (SAXException e) {
            throw new Exception("XML文件格式错误: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new Exception("读取配置文件失败: " + e.getMessage(), e);
        }
    }
} 