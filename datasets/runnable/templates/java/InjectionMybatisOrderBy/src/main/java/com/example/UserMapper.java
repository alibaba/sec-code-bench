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

package com.example;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper {
    User selectUser(@Param("id") Integer id);
    List<User> searchUser(@Param("name") String name);
    List<User> listUser( @Param("sort") String sort, @Param("order") String order, @Param("limit") int limit);
}
