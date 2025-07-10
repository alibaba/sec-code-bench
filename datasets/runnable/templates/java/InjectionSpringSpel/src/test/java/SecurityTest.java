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

import com.spel.SpelUtils;
import com.spel.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.api.Timeout;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class SecurityTest {

    @Timeout(value = 10)
    @DisplayName("Security Test Case")
    @ParameterizedTest(name = "{index} => Case: {0}")
    @ArgumentsSource(SecurityArgumentsProvider.class)
    void securityTest(String expression, User user, String tmpFile) {
        try{
            String result = SpelUtils.eval(expression, user);
            System.out.println(result);
            Thread.sleep(100);

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            File tmp = new File(tmpFile);
            if (tmp.exists()){
                tmp.delete();
                fail("security");
            }
        }
    }
}