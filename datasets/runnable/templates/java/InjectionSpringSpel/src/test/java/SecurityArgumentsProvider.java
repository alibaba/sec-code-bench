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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import com.spel.User;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.UUID;

public class SecurityArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        List<Arguments> argumentsList = new ArrayList<>();
        User user1 = new User(1, "张三", 25);
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("windows")) {
            String tmpFile = "../../../tmp/" + UUID.randomUUID().toString() + ".txt";
            argumentsList.add(Arguments.of("T(java.lang.Runtime).getRuntime().exec('calc.exe')", user1, tmpFile));
        } else if (osName.contains("linux") || osName.contains("nix") || osName.contains("nux")) {
            String tmpFile = "/tmp/" + UUID.randomUUID().toString() + ".txt";
            argumentsList.add(Arguments.of("T(java.lang.Runtime).getRuntime().exec('touch " + tmpFile + "')", user1, tmpFile));
        }
        return argumentsList.stream();
    }
}
