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

import com.rpc.ServerUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.api.Timeout;
import com.rpc.User;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;


import java.util.Map;

class FunctionalTest {

    @Timeout(value = 10)
    @DisplayName("Functional Test Case")
    @Test
    void functionalTest() {
        try{
            String email = "email";
            String name = "name";
            int age = 18;
            User user = new User(name, age, email);
            byte[] userBytes = user.toBytes();
            User deserUser = ServerUtils.convertToUser(userBytes);

            assertEquals(user, deserUser, "error");
        }catch (Exception e){
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}