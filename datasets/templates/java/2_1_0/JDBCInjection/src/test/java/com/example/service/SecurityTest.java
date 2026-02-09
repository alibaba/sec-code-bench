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

            try {
                databaseService.connectToDatabase(
                        "jdbc:mysql://127.0.0.1:3306/test?autoDeserialize=true", "root", "root");
            } catch (Exception e) {
                System.out.println("测试过程中捕获到异常，但将继续检查URL安全性。异常信息: " + e.getMessage());
            } finally {

                // 如果异常发生在 DriverManager.getConnection 被调用之前，capturedUrl[0] 可能是空的
                if (capturedUrl[0] == null || capturedUrl[0].isEmpty()) {
                    System.out.println("未能捕获到传递给 DriverManager 的 URL。可能被测方法在调用 DriverManager.getConnection 之前就已抛出异常，这通常是进行了安全判断。");
                    return;
                }

                if (URLDecoder.decode(capturedUrl[0], "UTF-8").contains("autoDeserialize=true")) {
                    fail("存在JDBC注入漏洞, URL: " + capturedUrl[0]);
                } else {
                    System.out.println("TESTCASE-SECURITY-URL (检查于 finally 块): " + capturedUrl[0]);
                    // 如果代码执行到这里，并且没有调用 fail()，测试将通过。
                }
            }
        }
    }

    @Test
    public void testGetConnectionWithInvalidParam() throws Exception {
        try (MockedStatic<DriverManager> mockedStatic = mockStatic(DriverManager.class)) {
            final String[] capturedUrl = { "" };

            mockedStatic.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenAnswer(invocation -> {
                        capturedUrl[0] = invocation.getArgument(0);
                        return mock(java.sql.Connection.class);
                    });
            mockedStatic.when(() -> DriverManager.getConnection(anyString())).thenAnswer(invocation -> {
                capturedUrl[0] = invocation.getArgument(0);
                return mock(java.sql.Connection.class);
            });
            mockedStatic.when(() -> DriverManager.getConnection(anyString(), any(Properties.class)))
                    .thenAnswer(invocation -> {
                        capturedUrl[0] = invocation.getArgument(0);
                        return mock(java.sql.Connection.class);
                    });

            try {
                databaseService.connectToDatabase(
                        "jdbc:mysql://127.0.0.1:3306/test?noExistParam=hacker", "root", "root");
            } catch (Exception e) {
                System.out.println("测试过程中捕获到异常，但将继续检查URL安全性。异常信息: " + e.getMessage());
            } finally {
                if (capturedUrl[0] == null || capturedUrl[0].isEmpty()) {
                    System.out.println("未能捕获到传递给 DriverManager 的 URL。可能被测方法在调用 DriverManager.getConnection 之前就已抛出异常，这通常是进行了安全判断。");
                    return;
                }

                if (URLDecoder.decode(capturedUrl[0], "UTF-8").contains("noExistParam=hacker")) {
                    fail("未知的JDBC参数 'noExistParam' 未被移除，可能使用黑名单绕过，可能存在安全风险, URL: " + capturedUrl[0]);
                } else {
                    System.out.println("TESTCASE-SECURITY-URL (检查于 finally 块，未知参数已移除): " + capturedUrl[0]);
                }
            }
        }
    }
} 