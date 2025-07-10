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

import java.util.*;
import java.util.stream.Stream;

public class SecurityArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        List<Arguments> argumentsList = new ArrayList<>();

        String tmpFile = "/tmp/" + UUID.randomUUID().toString() + ".txt";
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Alice");
        data.put("place", "Wonderland");
        String template = "#set($e=\"exp\"+\"and\")\n" +
                "$e.getClass().forName(\"java.lang.Runtime\").getMethod(\"getRuntime\",null).invoke(null,null).exec('touch " + tmpFile + "')";
        argumentsList.add(Arguments.of(template, data, tmpFile));

        return argumentsList.stream();
    }
}
