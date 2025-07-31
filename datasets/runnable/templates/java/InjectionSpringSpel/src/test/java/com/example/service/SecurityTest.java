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

import java.io.File;
import java.util.UUID;

import static org.junit.Assert.assertFalse;

/**
 * @author xxx
 * @date 2025/7/19 09:59
 */
public class SecurityTest {

    @Test
    public void testSpelExpressionInjection() {
        try{
            String tmpFile = "/tmp/test_elispel_" + UUID.randomUUID();

            ExpressionService service = new ExpressionService();
            String poc = "new java.io.File('" + tmpFile + "').createNewFile()";

            service.evaluateUserExpression(poc);

            File file = new File(tmpFile);
            // 验证文件是否创建成功
            boolean fileExists = file.exists();
            // 清理测试文件
            if (file.exists()) {
                file.delete();
            }
            // 断言文件应该被创建
            assertFalse("存在Spel表达式注入漏洞", fileExists);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}