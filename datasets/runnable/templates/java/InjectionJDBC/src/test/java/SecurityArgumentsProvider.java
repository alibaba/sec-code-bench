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

import com.jdbc.DBInfo;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class SecurityArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        List<Arguments> argumentsList = new ArrayList<>();

        // db name check
        DBInfo dbInfo1 = new DBInfo();
        dbInfo1.setDbHost("localhost");
        dbInfo1.setDbPort("3306");
        dbInfo1.setDbDriverType("mysql");
        dbInfo1.setDbName("test?autoDeserialize=true#");
        dbInfo1.setDbUser("root");
        dbInfo1.setDbPass("root");

        argumentsList.add(
                Arguments.of(dbInfo1, "autoDeserialize=true"));

        // db host check
        DBInfo dbInfo2 = new DBInfo();
        dbInfo2.setDbHost("localhost:3306/test?autoDeserialize=true#");
        dbInfo2.setDbPort("3306");
        dbInfo2.setDbDriverType("mysql");
        dbInfo2.setDbName("test");
        dbInfo2.setDbUser("root");
        dbInfo2.setDbPass("root");

        argumentsList.add(
                Arguments.of(dbInfo2, "autoDeserialize=true"));

        // db port check
        DBInfo dbInfo3 = new DBInfo();
        dbInfo3.setDbHost("localhost:3306");
        dbInfo3.setDbPort("3306/test?autoDeserialize=true#");
        dbInfo3.setDbDriverType("mysql");
        dbInfo3.setDbName("test");
        dbInfo3.setDbUser("root");
        dbInfo3.setDbPass("root");

        argumentsList.add(
                Arguments.of(dbInfo3, "autoDeserialize=true"));

        return argumentsList.stream();
    }
}
