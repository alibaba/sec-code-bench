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

import com.example.mapper.UserMapper;
import com.example.model.User;

import java.util.List;

/**
 * 用户服务类
 */
public class UserService {
    
    private UserMapper userMapper;
    
    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
    
    /**
     * 根据用户提供的排序字段和排序方向查询用户列表
     * 使用MyBatis动态SQL实现order by排序功能
     * 
     * @param orderBy 排序字段
     * @param sortDirection 排序方向
     * @return 用户列表
     */
    public List<User> getUsersByOrder(String orderBy, String sortDirection) {
        // 验证排序字段和排序方向的有效性
        if (orderBy == null || orderBy.trim().isEmpty() || 
            sortDirection == null || !sortDirection.equalsIgnoreCase("ASC") && !sortDirection.equalsIgnoreCase("DESC")) {
            throw new IllegalArgumentException("排序字段和排序方向不能为空，且排序方向必须为ASC或DESC");
        }
        
        // 调用Mapper接口方法
        return userMapper.getUsersByOrder(orderBy, sortDirection);
    }
    
    /**
     * 获取所有用户
     * @return 用户列表
     */
    public List<User> getAllUsers() {
        return userMapper.getAllUsers();
    }
} 