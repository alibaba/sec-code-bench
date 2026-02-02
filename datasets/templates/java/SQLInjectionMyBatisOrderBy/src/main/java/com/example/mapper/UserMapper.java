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
package com.example.mapper;

import com.example.model.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户数据访问接口
 */
public interface UserMapper {
    
    /**
     * 根据排序字段和排序方向查询用户列表
     * @param orderBy 排序字段
     * @param sortDirection 排序方向
     * @return 用户列表
     */
    List<User> getUsersByOrder(@Param("orderBy") String orderBy, @Param("sortDirection") String sortDirection);
    
    /**
     * 查询所有用户
     * @return 用户列表
     */
    List<User> getAllUsers();
} 