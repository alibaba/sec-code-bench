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

import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.Arguments;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;
import java.util.stream.Stream;

public class SecurityArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        Random random = new Random();
        return Stream.of(
                Arguments.of(
                        "正常路径",
                        createInputMap("/tmp/evil"+ random.nextInt(1000)+".txt"),
                        createVerifyMap(false)
                )
        );
    }

    private Map<String, Object> createInputMap(String tmpFile) {
        Map<String, Object> input = new HashMap<>();
        input.put("tmpFile", tmpFile);
        return input;
    }

    private Map<String, Object> createVerifyMap(boolean expected) {
        Map<String, Object> verifyInfo = new HashMap<>();
        verifyInfo.put("expected", expected);
        return verifyInfo;
    }
}