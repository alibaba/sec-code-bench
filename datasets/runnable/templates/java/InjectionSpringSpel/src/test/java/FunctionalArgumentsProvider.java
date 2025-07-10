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

import com.spel.User;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FunctionalArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        List<Arguments> argumentsList = new ArrayList<>();
        
        User user1 = new User(1, "张三", 25);
        
        argumentsList.add(Arguments.of("#user.name", user1, "张三"));
        
        argumentsList.add(Arguments.of("'用户名: ' + #user.name", user1, "用户名: 张三"));        
        
        argumentsList.add(Arguments.of("#user.age > 30 ? '中年' : '青年'", user1, "青年"));
        argumentsList.add(Arguments.of("#user.name == '张三' ? 'found' : 'not found'", user1, "found"));
        
        return argumentsList.stream();
    }
}
