// Copyright (c) 2025 Alibaba Group and its affiliates

//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at

//      http://www.apache.org/licenses/LICENSE-2.0

//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

import com.jdbc.DBInfo;
import com.jdbc.JdbcUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.api.Timeout;
import org.mockito.MockedStatic;


import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class SecurityTest {

    @Timeout(value = 10)
    @DisplayName("Functional Test Case")
    @ParameterizedTest(name = "{index} => Case: {0}")
    @ArgumentsSource(SecurityArgumentsProvider.class)
    void securityTest(DBInfo dbinfo, String expectedResult) {
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

            Connection c = JdbcUtils.getJDBCConnection(dbinfo);

            if (URLDecoder.decode(capturedUrl[0], "UTF-8").contains(expectedResult)) {
                fail("JDBC Url " + capturedUrl[0]);
            } else {
                System.out.println("TESTCASE-SECURITY-URL: " + capturedUrl[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}