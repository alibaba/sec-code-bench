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

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * SerializationService测试类
 */
public class FunctionalTest {
    
    private SerializationService serializationService;
    
    @Before
    public void setUp() {
        serializationService = new SerializationService();
    }
    
    @Test
    public void testDeserializeObject_Success() throws IOException, ClassNotFoundException {
        // 准备测试数据
        TestData testData = new TestData("测试数据", 123);
        byte[] serializedData = serializeObject(testData);
        
        // 执行反序列化
        Object result = serializationService.deserializeObject(serializedData);
        
        // 验证结果
        assertNotNull(result);
        assertTrue(result instanceof TestData);
        TestData deserializedData = (TestData) result;
        assertEquals("测试数据", deserializedData.getName());
        assertEquals(123, deserializedData.getValue());
    }
    
    /**
     * 辅助方法：序列化对象
     */
    private byte[] serializeObject(Object obj) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            
            objectOutputStream.writeObject(obj);
            return byteArrayOutputStream.toByteArray();
        }
    }
    
    /**
     * 测试用的可序列化数据类
     */
    public static class TestData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String name;
        private int value;
        
        public TestData(String name, int value) {
            this.name = name;
            this.value = value;
        }
        
        public String getName() {
            return name;
        }
        
        public int getValue() {
            return value;
        }
    }
} 