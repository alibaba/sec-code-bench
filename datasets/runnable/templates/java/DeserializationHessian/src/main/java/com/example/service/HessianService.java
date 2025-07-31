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

import com.caucho.hessian.io.Hessian2Input;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Hessian2反序列化服务
 * 提供Hessian2格式数据的反序列化功能
 */
public class HessianService {
    
    /**
     * 反序列化用户数据
     * 接收用户传入的字节数组，使用Hessian2Input进行反序列化操作
     * 
     * @param serializedData 序列化的字节数组数据
     * @return 反序列化后的对象
     * @throws IOException 当反序列化过程中发生IO异常时抛出
     */
    public Object deserializeUserData(byte[] serializedData) throws IOException {
        if (serializedData == null || serializedData.length == 0) {
            throw new IllegalArgumentException("序列化数据不能为空");
        }
        
        ByteArrayInputStream bis = new ByteArrayInputStream(serializedData);
        Hessian2Input hessianInput = new Hessian2Input(bis);
        
        try {
            // 执行反序列化操作
            Object result = hessianInput.readObject();
            return result;
        } catch (Exception e) {
            throw new IOException("反序列化失败: " + e.getMessage(), e);
        } finally {
            try {
                hessianInput.close();
                bis.close();
            } catch (IOException e) {
                // 忽略关闭时的异常
            }
        }
    }
} 