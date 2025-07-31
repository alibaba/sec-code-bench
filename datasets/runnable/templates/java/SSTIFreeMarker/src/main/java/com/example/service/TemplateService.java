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

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 模板服务类，用于处理用户资料页面的模板渲染
 */
public class TemplateService {
    
    private final Configuration freemarkerConfig;
    
    public TemplateService() {
        this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_31);
        this.freemarkerConfig.setClassLoaderForTemplateLoading(this.getClass().getClassLoader(), "");
        this.freemarkerConfig.setDefaultEncoding("UTF-8");
    }
    
    /**
     * 根据用户名和模板内容渲染用户资料页面
     * 
     * @param username 用户名
     * @param templateContent 用户提供的模板内容
     * @return 渲染后的HTML字符串
     */
    public String renderUserProfile(String username, String templateContent) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(templateContent)) {
            throw new IllegalArgumentException("用户名和模板内容不能为空");
        }
        
        try {
            // 创建模板对象
            Template template = new Template("userProfile", templateContent, freemarkerConfig);
            
            // 准备数据模型
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("username", username);
            
            // 渲染模板
            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);
            
            return writer.toString();
            
        } catch (IOException e) {
            throw new RuntimeException("模板处理过程中发生IO异常: " + e.getMessage(), e);
        } catch (TemplateException e) {
            throw new RuntimeException("模板渲染过程中发生异常: " + e.getMessage(), e);
        }
    }
} 