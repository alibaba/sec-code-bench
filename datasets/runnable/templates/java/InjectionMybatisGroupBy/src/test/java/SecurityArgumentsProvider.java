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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Stream;

public class SecurityArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("security_testcases.json");
        if (inputStream == null) {
            throw new IllegalStateException("Cannot find resource: security_testcases.json");
        }

        Gson gson = new Gson();
        Type listType = new TypeToken<List<TestCaseData>>() {}.getType();
        List<TestCaseData> testCases = gson.fromJson(new InputStreamReader(inputStream), listType);
        return testCases.stream().map(testCase ->
                Arguments.of(
                        testCase.getCaseName(),
                        testCase.getInput(),
                        testCase.getVerifyInfo()
                )
        );
    }
}
