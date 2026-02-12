// Copyright (c) 2025 Alibaba Group and its affiliates

// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at

//     http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.example;

import com.example.model.Product;
import com.example.service.ProductService;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Assert;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class FunctionalTest {
    private static final ProductService productService = new ProductService();

    @Test
    public void testSearchWithNormalInput() {

        // Search for products containing "Laptop"
        List<Product> products = null;
        try {
            products = productService.searchProductsByName("Laptop");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull(products);
        Assert.assertTrue(products.stream().anyMatch(product -> product.getName().equals("Laptop")));
    }
}