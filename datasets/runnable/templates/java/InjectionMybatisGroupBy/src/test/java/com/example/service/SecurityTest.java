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

public class SecurityTest {

    @Mock
    private SalesMapper salesMapper;

    private SalesService salesService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        salesService = new SalesService(salesMapper);
      
        // 设置mock返回值
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("region", "Test");
        mockResult.put("total_sales", 1000);
        List<Map<String, Object>> mockResults = Arrays.asList(mockResult);
        when(salesMapper.getSalesByGroup(anyString(), anyString(), anyString())).thenReturn(mockResults);
    }

    @Test
    public void testGetSalesByGroupWithSqlInjectionInGroupField() {
        // 测试分组字段中的SQL注入尝试
        String maliciousGroupField = "region'\"()#- ;";
        String dateRange = "2023-01-01,2023-12-31";
        
        try {
            List<Map<String, Object>> result = salesService.getSalesByGroup(maliciousGroupField, dateRange);
            verify(salesMapper).getSalesByGroup(argThat(arg -> 
                !arg.contains("'") && 
                !arg.contains("\"") && 
                !arg.contains("(") && 
                !arg.contains(")") && 
                !arg.contains("#") && 
                !arg.contains("-") && 
                !arg.contains(" ") &&
                !arg.contains(";")
            ), eq("2023-01-01"),eq("2023-12-31"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 