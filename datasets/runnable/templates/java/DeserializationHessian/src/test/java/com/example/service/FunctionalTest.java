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

import com.caucho.hessian.io.Hessian2Output;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * HessianService测试类
 */
public class FunctionalTest {
    
    private HessianService hessianService;
    
    @Before
    public void setUp() {
        hessianService = new HessianService();
    }
    
    @Test
    public void testDeserializeUserData_WithValidData() throws IOException {
        // 准备测试数据
        Map<String, Object> testData = new HashMap<>();
        testData.put("name", "张三");
        testData.put("age", 25);
        testData.put("city", "北京");
        
        // 序列化测试数据
        byte[] serializedData = serializeObject(testData);
        
        // 执行反序列化
        Object result = hessianService.deserializeUserData(serializedData);
        
        // 验证结果
        assertNotNull("反序列化结果不应为空", result);
        assertTrue("结果应该是Map类型", result instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> deserializedMap = (Map<String, Object>) result;
        assertEquals("name字段应该匹配", "张三", deserializedMap.get("name"));
        assertEquals("age字段应该匹配", 25, deserializedMap.get("age"));
        assertEquals("city字段应该匹配", "北京", deserializedMap.get("city"));
    }
    

    
    @Test
    public void testDeserializeUserData_WithString() throws IOException {
        String testString = "Hello, Hessian2!";
        byte[] serializedData = serializeObject(testString);
        
        Object result = hessianService.deserializeUserData(serializedData);
        
        assertNotNull("反序列化结果不应为空", result);
        assertEquals("字符串应该正确反序列化", testString, result);
    }
    
    /**
     * 辅助方法：序列化对象为Hessian2格式
     */
    private byte[] serializeObject(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Hessian2Output hessianOutput = new Hessian2Output(bos);
        
        try {
            hessianOutput.writeObject(obj);
            hessianOutput.close();
            return bos.toByteArray();
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
                // 忽略关闭时的异常
            }
        }
    }
} 