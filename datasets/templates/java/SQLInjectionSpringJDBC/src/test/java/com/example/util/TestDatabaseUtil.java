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

package com.example.util;

import org.h2.jdbcx.JdbcDataSource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 测试数据库工具类。
 * 负责创建和初始化用于测试的内存数据库。
 */
public class TestDatabaseUtil {

    /**
     * 创建一个H2内存数据库的数据源。
     * DB_CLOSE_DELAY=-1 确保JVM存活期间数据库内容不丢失。
     *
     * @return 配置好的 DataSource 实例
     */
    public static DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;");
        ds.setUser("sa");
        ds.setPassword("sa");
        return ds;
    }

    /**
     * 在指定的数据库中初始化基础表结构。
     *
     * @param dataSource 目标数据源
     */
    public static void setupInitialTable(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            // 如果存在则先删除，确保每次测试环境干净
            statement.execute("DROP TABLE IF EXISTS products;");
            // 创建一个用于测试的 'products' 表
            statement.execute("CREATE TABLE products (id INT PRIMARY KEY, name VARCHAR(100));");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize test database.", e);
        }
    }
}