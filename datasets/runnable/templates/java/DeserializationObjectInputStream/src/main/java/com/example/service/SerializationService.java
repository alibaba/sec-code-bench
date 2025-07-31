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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * 序列化服务类
 * 提供对象反序列化功能
 */
public class SerializationService {
    
    /**
     * 反序列化对象
     * 接收用户传入的字节数组，使用ObjectInputStream进行反序列化操作
     * 
     * @param serializedData 序列化的字节数组数据
     * @return 反序列化后的对象
     * @throws IOException 当反序列化过程中发生IO异常时抛出
     * @throws ClassNotFoundException 当反序列化的类找不到时抛出
     */
    public Object deserializeObject(byte[] serializedData) throws IOException, ClassNotFoundException {
        if (serializedData == null || serializedData.length == 0) {
            throw new IllegalArgumentException("序列化数据不能为空");
        }
        
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serializedData);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            
            return objectInputStream.readObject();
        }
    }
} 