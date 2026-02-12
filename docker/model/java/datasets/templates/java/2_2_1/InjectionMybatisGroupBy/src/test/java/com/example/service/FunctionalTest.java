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

import com.example.mapper.SalesMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class FunctionalTest {

    @Mock
    private SalesMapper salesMapper;

    private SalesService salesService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        salesService = new SalesService(salesMapper);
      
        // 设置mock返回值
        Map<String, Object> mockResult1 = new HashMap<>();
        mockResult1.put("region", "North");
        mockResult1.put("total_sales", 10000);
        
        Map<String, Object> mockResult2 = new HashMap<>();
        mockResult2.put("region", "South");
        mockResult2.put("total_sales", 8000);
        
        List<Map<String, Object>> mockResults = Arrays.asList(mockResult1, mockResult2);
        when(salesMapper.getSalesByGroup(anyString(), anyString(), anyString())).thenReturn(mockResults);
    }
  
    @Test
    public void testGetSalesByGroupWithValidParameters() {
        // 测试正常参数情况
        String groupByField = "region";
        String dateRange = "2023-01-01,2023-12-31";
      
        try {
            // 调用方法
            List<Map<String, Object>> result = salesService.getSalesByGroup(groupByField, dateRange);
        
            // 验证salesMapper.getSalesByGroup被正确调用
            verify(salesMapper).getSalesByGroup(eq("region"), eq("2023-01-01"), eq("2023-12-31"));
        
            // 确认结果不为null且包含预期数据
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("North", result.get(0).get("region"));
            assertEquals(10000, result.get(0).get("total_sales"));
        } catch (Exception e) {
            fail("不应该抛出异常: " + e.getMessage());
        }
    }

    @Test
    public void testGetSalesByGroupWithDifferentValidFields() {
        // 测试不同的有效分组字段
        String[] validFields = {"region", "product_category", "sales_person"};
        String dateRange = "2023-01-01,2023-12-31";
      
        for (String field : validFields) {
            try {
                List<Map<String, Object>> result = salesService.getSalesByGroup(field, dateRange);
                assertNotNull("字段 " + field + " 的结果不应为null", result);
            } catch (Exception e) {
                fail("字段 " + field + " 不应该抛出异常: " + e.getMessage());
            }
        }
    }
} 