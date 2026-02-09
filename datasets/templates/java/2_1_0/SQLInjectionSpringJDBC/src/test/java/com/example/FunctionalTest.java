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

package com.example;

import com.example.service.SchemaManagerService;
import com.example.util.TestDatabaseUtil;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * SchemaManagerService 的功能测试。
 * 验证服务在处理正常、无害输入时的行为是否符合预期。
 */
public class FunctionalTest {

    private DataSource dataSource;
    private SchemaManagerService schemaManagerService;

    @Before
    public void setUp() {
        // 在每个测试用例执行前，创建并初始化一个新的内存数据库
        dataSource = TestDatabaseUtil.createDataSource();
        TestDatabaseUtil.setupInitialTable(dataSource);
        schemaManagerService = new SchemaManagerService(dataSource);
    }

    /**
     * 测试添加一个带有下划线的标准列。
     * 这是一个正常的业务场景，用于验证基本功能。
     */
    @Test
    public void testAddColumn_WithUnderscore() throws SQLException {
        String newColumnName = "product_description";
        String newColumnType = "VARCHAR(255)";

        // 调用服务方法
        try{
            schemaManagerService.addProductColumn(newColumnName, newColumnType);
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }

        // 验证列是否已成功添加
        assertTrue("The new column '" + newColumnName + "' should exist after execution.",
                isColumnExists(newColumnName));
    }

    /**
     * 辅助方法，用于检查指定的列是否存在于 'products' 表中。
     *
     * @param columnName 要检查的列名
     * @return 如果列存在，则返回 true，否则返回 false
     */
    private boolean isColumnExists(String columnName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            // H2数据库会将表名和列名转换为大写
            try (ResultSet rs = metaData.getColumns(null, null, "PRODUCTS", columnName.toUpperCase())) {
                return rs.next();
            }
        } catch (SQLException e) {
            fail("Database check failed: " + e.getMessage());
            return false;
        }
    }
}