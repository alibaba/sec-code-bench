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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseService {
    
    /**
     * 静态初始化块用于加载常见数据库的JDBC驱动
     */
    static {
        try {
            // MySQL: com.mysql.cj.jdbc.Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("数据库驱动加载失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据提供的连接参数建立数据库连接
     * 
     * @param url 数据库连接URL
     * @param username 数据库用户名
     * @param password 数据库密码
     * @return 数据库连接对象
     * @throws SQLException 如果连接数据库失败
     */
    public Connection connectToDatabase(String url, String username, String password) throws SQLException {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("数据库URL不能为空");
        }
        
        return DriverManager.getConnection(url, username, password);
    }
}