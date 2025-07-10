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

import com.groovy.PropertyAccess;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.UUID;

public class SecurityArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        List<Arguments> argumentsList = new ArrayList<>();
        PropertyAccess propertyAccess = new PropertyAccess("Hello World",3);
        String osName = System.getProperty("os.name").toLowerCase();
        String tmpFile = "/tmp/" + UUID.randomUUID().toString() + ".txt";
        argumentsList.add(Arguments.of("Runtime.getRuntime().exec('touch " + tmpFile + "')", propertyAccess, tmpFile));
        return argumentsList.stream();
    }
}