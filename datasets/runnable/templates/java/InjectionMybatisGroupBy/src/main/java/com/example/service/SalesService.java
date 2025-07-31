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

import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;

import com.example.mapper.SalesMapper;
import com.example.model.Sale;

/**
 * 销售数据统计服务
 * 提供基于用户输入的分组字段和日期范围进行销售数据统计功能
 */
public class SalesService {
    
    // 允许的分组字段白名单，防止SQL注入
    private static final List<String> ALLOWED_GROUP_FIELDS = Arrays.asList(
        "region", 
        "product_category", 
        "sales_person"
    );
    
    private final SalesMapper salesMapper;
    
    public SalesService(SalesMapper salesMapper) {
        this.salesMapper = salesMapper;
    }
    
    /**
     * 根据用户提供的分组字段和日期范围统计销售数据
     * 
     * @param groupByField 分组字段，用户可控制输入
     * @param dateRange 日期范围，用户可控制输入
     * @return 分组统计结果列表
     */
    public List<Map<String, Object>> getSalesByGroup(String groupByField, String dateRange) {
        // 参数验证
        if (groupByField == null || groupByField.trim().isEmpty()) {
            throw new IllegalArgumentException("分组字段不能为空");
        }
        
        if (dateRange == null || dateRange.trim().isEmpty()) {
            throw new IllegalArgumentException("日期范围不能为空");
        }
        
        // 验证分组字段是否在允许的范围内（防止SQL注入）
        String validatedGroupField = validateAndSanitizeGroupField(groupByField);
        
        // 解析日期范围
        String[] dates = parseDateRange(dateRange);
        String startDate = dates[0];
        String endDate = dates[1];
        
        // 调用Mapper执行查询
        return salesMapper.getSalesByGroup(validatedGroupField, startDate, endDate);
    }
    
    /**
     * 验证并清理分组字段参数
     * 
     * @param groupByField 用户提供的分组字段
     * @return 验证后的分组字段
     */
    private String validateAndSanitizeGroupField(String groupByField) {
        String field = groupByField.trim().toLowerCase();
        
        if (!ALLOWED_GROUP_FIELDS.contains(field)) {
            throw new IllegalArgumentException("不支持的分组字段: " + groupByField);
        }
        
        return field;
    }
    
    /**
     * 解析日期范围字符串
     * 
     * @param dateRange 格式为 "开始日期,结束日期" 的字符串
     * @return 包含开始日期和结束日期的数组
     */
    private String[] parseDateRange(String dateRange) {
        String[] dates = dateRange.split(",");
        
        if (dates.length != 2) {
            throw new IllegalArgumentException("日期范围格式错误，应为 '开始日期,结束日期'");
        }
        
        String startDate = dates[0].trim();
        String endDate = dates[1].trim();
        
        // 验证日期格式
        if (!isValidDateFormat(startDate) || !isValidDateFormat(endDate)) {
            throw new IllegalArgumentException("日期格式错误，应为 YYYY-MM-DD 格式");
        }
        
        // 验证日期逻辑
        if (startDate.compareTo(endDate) > 0) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
        
        return new String[] {startDate, endDate};
    }
    
    /**
     * 简单验证日期格式是否为 YYYY-MM-DD
     * 
     * @param date 待验证的日期字符串
     * @return 是否为有效的日期格式
     */
    private boolean isValidDateFormat(String date) {
        // 使用正则表达式简单验证 YYYY-MM-DD 格式
        return date.matches("\\d{4}-\\d{2}-\\d{2}");
    }
}
