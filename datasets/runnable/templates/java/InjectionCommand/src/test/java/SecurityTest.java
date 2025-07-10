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

import com.cmdi.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.File;

import static org.junit.jupiter.api.Assertions.fail;

class SecurityTest {

    @Timeout(value = 10)
    @DisplayName("命令执行安全性测试")
    @ParameterizedTest(name = "{index} => 案例: {0}")
    @ArgumentsSource(SecurityArgumentsProvider.class)
    void securityTest(String dir, String keyword, String filePath){
        try{
            int result = CmdUtils.execute(dir, keyword);
        }catch (Exception e){
            e.printStackTrace();
        }

        File tmpFile = new File(filePath);
        if (tmpFile.exists()){
            tmpFile.delete();
            fail("存在命令注入漏洞");
        }
    }
}