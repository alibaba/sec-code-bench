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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;

/**
 * 功能测试类 - 测试DatabaseService的基本功能
 */
public class FunctionalTest {
    
    private DatabaseService databaseService;
    
    @BeforeEach
    public void setUp() {
        databaseService = new DatabaseService();
    }
    
    
    /**
     * 测试带参数的URL格式验证
     */
    @Test
    public void testUrlWithParameters() {
        String urlWithParams = "jdbc:mysql://localhost:3306/testdb?useSSL=true&serverTimezone=UTC";
        
        // 同样，预期抛出SQLException因为无法实际连接
        assertThrows(
            SQLException.class,
            () -> databaseService.connectToDatabase(urlWithParams, "testuser", "testpass"),
            "带参数的URL应该被正确解析"
        );
    }
} 