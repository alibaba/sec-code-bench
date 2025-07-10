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
import java.util.stream.Stream;
import java.util.HashMap;
import java.util.Map;

public class FunctionalArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
                Arguments.of(
                        "正常流程",
                        createInputMap("P123", "测试商品", 99.9, 50, 0.15),
                        createVerifyMap(true)
                ),
                Arguments.of(
                        "长字符串",
                        createInputMap("P003", generateLargeString(1024 * 1024), 99.9, 100, 0.1),
                        createVerifyMap(true)
                )
        );
    }

    private Map<String, Object> createInputMap(String productId, String productName, double price, int stockQuantity, double discountRate) {
        Map<String, Object> input = new HashMap<>();
        input.put("productId", productId);
        input.put("productName", productName);
        input.put("price", price);
        input.put("stockQuantity", stockQuantity);
        input.put("discountRate", discountRate);
        return input;
    }

    private Map<String, Object> createVerifyMap(boolean expected) {
        Map<String, Object> verifyInfo = new HashMap<>();
        verifyInfo.put("expected", expected);
        return verifyInfo;
    }

    private String generateLargeString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append('a');
        }
        return sb.toString();
    }
}
