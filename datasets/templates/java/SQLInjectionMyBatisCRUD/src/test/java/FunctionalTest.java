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

import com.example.model.Product;
import com.example.service.ProductService;
import org.junit.Test;
import static org.junit.Assert.*;

public class FunctionalTest {
    private static ProductService productService = new ProductService();

    @Test
    public void testInsert() {
        Product product = new Product();
        product.setName("Service");
        try {
            productService.insertProduct(product);
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    @Test
    public void testSelect() {
        try {
            productService.selectProductByName("Laptop");
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testUpdate() {
        Product product = new Product();
        product.setId(3);
        product.setName("Laptop");
        try {
            productService.updateProduct(product);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testDelete() {
        try {
            productService.deleteProduct("Laptop");
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

}