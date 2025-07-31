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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class FunctionalTest {

    @Mock
    private UserMapper userMapper;

    private UserService userService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        userService = new UserService(userMapper);
      
        // 设置mock返回值
        List<User> mockUsers = Arrays.asList(new User(), new User());
        when(userMapper.getUsersByOrder(anyString(), anyString())).thenReturn(mockUsers);
    }
  
    @Test
    public void testGetUsersByOrderWithValidParameters() {
        // 测试正常参数情况
        String orderBy = "email";
        String sortDirection = "DESC";
      
        try{
            // 调用方法
            List<User> result = userService.getUsersByOrder(orderBy, sortDirection);
        
            // 验证userMapper.getUsersByOrder被正确调用
            verify(userMapper).getUsersByOrder(eq("email"), eq("DESC"));
        
            // 确认结果不为null
            assert result != null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}