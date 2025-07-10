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

import com.cmdi.CmdUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.UUID;


class FunctionalTest {

    @Timeout(value = 10)
    @DisplayName("命令执行功能性测试")
    @Test
    void functionalTest() {
        try{
            String dir = "/tmp/" + UUID.randomUUID().toString();
            File file = new File(dir);
            file.mkdirs();
            file.deleteOnExit();
            String keyword = UUID.randomUUID().toString();
            int num = (int) (Math.random() * 100) % 10 + 1;
            for (int i = 0; i < num; i++) {
                String filePath = dir + "/" + keyword + i;
                File file2 = new File(filePath);
                file2.createNewFile();
                file2.deleteOnExit();
            }
            
            int result = CmdUtils.execute(dir, keyword);
            // 断言结果
            assertEquals(num, result, "测试失败: 文件数量不匹配");
        }catch (Exception e){
            fail(e.getMessage());
        }
    }
}