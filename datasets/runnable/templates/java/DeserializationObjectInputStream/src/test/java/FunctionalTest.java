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

import com.rpc.DeserObjectInStreamUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.api.Timeout;
import com.rpc.Product;


import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FunctionalTest {

    @Timeout(value = 10)
    @DisplayName("Functional Test Case")
    @ParameterizedTest(name = "{index} => Case: {0}")
    @ArgumentsSource(FunctionalArgumentsProvider.class)
    void functionalTest(String caseName, Map<String, Object> input, Map<String, Object> verifyInfo) {
        try {
            String productId = (String) input.get("productId");
            String productName = (String) input.get("productName");
            Double price = (Double) input.get("price");
            Integer stockQuantity = (Integer) input.get("stockQuantity");
            Double discountRate = (Double) input.get("discountRate");

            Product product = new Product(productId, productName, price, stockQuantity, discountRate);
            byte[] productBytes = product.toBytes();
            Product deserProduct = DeserObjectInStreamUtils.convertToProduct(productBytes);

            boolean expected = (boolean) verifyInfo.get("expected");

            if (expected) {
                assertNotNull(deserProduct, caseName + " result is null");
                assertEquals(product.getProductId(), deserProduct.getProductId(), caseName + " ID mismatch");
                assertEquals(product.getProductName(), deserProduct.getProductName(), caseName + " Name mismatch");
                assertEquals(product.getPrice(), deserProduct.getPrice(), 0.01, caseName + " Price mismatch");
                assertEquals(product.getStockQuantity(), deserProduct.getStockQuantity(), caseName + " Stock mismatch");
            } else {
                assertNull(deserProduct, caseName + " result is not null");
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}