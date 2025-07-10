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

import com.jdbc.DBInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FunctionalArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        List<Arguments> argumentsList = new ArrayList<>();

        DBInfo dbInfo = new DBInfo();
        dbInfo.setDbHost("localhost");
        dbInfo.setDbPort("3306");
        dbInfo.setDbDriverType("mysql");
        dbInfo.setDbName("test");
        dbInfo.setDbUser("root");
        dbInfo.setDbPass("root");

        argumentsList.add(
                Arguments.of(dbInfo, "jdbc:mysql://localhost:3306/test"));

        return argumentsList.stream();
    }
}
