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

import org.yaml.snakeyaml.Yaml;

/**
 * YAML解析服务
 * 提供YAML字符串解析功能，支持用户输入的YAML内容解析
 */
public class YamlService {
    
    private final Yaml yaml;
    
    public YamlService() {
        this.yaml = new Yaml();
    }
    
    /**
     * 解析用户输入的YAML字符串
     * 
     * @param yamlContent 用户输入的YAML内容字符串
     * @return 解析后的对象
     */
    public Object parseUserYaml(String yamlContent) {
        if (yamlContent == null || yamlContent.trim().isEmpty()) {
            throw new IllegalArgumentException("YAML内容不能为空");
        }
        
        try {
            return yaml.load(yamlContent);
        } catch (Exception e) {
            throw new RuntimeException("YAML解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取YAML解析器实例
     * 
     * @return Yaml实例
     */
    public Yaml getYaml() {
        return yaml;
    }
} 