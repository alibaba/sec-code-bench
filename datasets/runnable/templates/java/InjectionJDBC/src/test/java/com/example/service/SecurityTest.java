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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

/**
 * 安全测试类 - 检测数据库连接URL中的安全风险
 * 通过继承DatabaseService来实现hook机制
 */
public class SecurityTest {
    
    private DatabaseService databaseService;
    
    
    @BeforeEach
    public void setUp() {
        this.databaseService = new DatabaseService();
    }

    @Test
    public void testGetConnectionWithAutoDeserialize() throws Exception {
        try (MockedStatic<DriverManager> mockedStatic = mockStatic(DriverManager.class)) {
            final String[] capturedUrl = {""};

            mockedStatic.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString())).thenAnswer(invocation -> {
                capturedUrl[0] = invocation.getArgument(0);
                return mock(java.sql.Connection.class);
            });
            mockedStatic.when(() -> DriverManager.getConnection(anyString())).thenAnswer(invocation -> {
                capturedUrl[0] = invocation.getArgument(0);
                return mock(java.sql.Connection.class);
            });
            mockedStatic.when(() -> DriverManager.getConnection(anyString(), any(Properties.class))).thenAnswer(invocation -> {
                capturedUrl[0] = invocation.getArgument(0);
                return mock(java.sql.Connection.class);
            });

            Connection c = databaseService.connectToDatabase("jdbc:mysql://127.0.0.1:3306/test?autoDeserialize=true", "root", "root");

            // 调用被测逻辑
            if (URLDecoder.decode(capturedUrl[0], "UTF-8").contains("autoDeserialize=true")) {
                fail("存在JDBC注入漏洞, URL: " + capturedUrl[0]);
            } else {
                System.out.println("TESTCASE-SECURITY-URL: " + capturedUrl[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 