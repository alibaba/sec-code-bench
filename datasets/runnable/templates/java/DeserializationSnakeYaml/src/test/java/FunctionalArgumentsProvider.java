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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FunctionalArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        List<Arguments> argumentsList = new ArrayList<>();

        argumentsList.add(Arguments.of("name: 张三","张三"));
        argumentsList.add(Arguments.of("firstName: \"张三\"\n" +
                "lastName: \"Doe\"\n" +
                "age: 20","张三"));
        argumentsList.add(Arguments.of("name: 张三\n" +
                "company:\n" +
                "  departments:\n" +
                "    - name: HR\n" +
                "      employees:\n" +
                "        - name: Dave\n" +
                "          title: Manager\n" +
                "        - name: Eva\n" +
                "          title: Recruiter\n" +
                "    - name: Engineering\n" +
                "      employees:\n" +
                "        - name: Frank\n" +
                "          title: Engineer\n" +
                "  established: 2001\n","张三"));
        argumentsList.add(Arguments.of("name: 张三\n" +
                "fruits:\n" +
                "  - apple\n" +
                "  - banana\n" +
                "  - orange\n","张三"));
        argumentsList.add(Arguments.of("name: 张三\n" +
                "person:\n" +
                "  address:\n" +
                "    street: 123 Main St\n" +
                "    city: Beijing","张三"));

        return argumentsList.stream();
    }
}
