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
import org.junit.Assert;
import org.junit.Test;

/**
 * @author xxx
 * @date 2025/7/19 09:59
 */
public class SecurityTest {
    private static ProductService productService = new ProductService();

    @Test
    public void testInsert() {
        try {
            Product product = new Product();
            product.setName("Service'");
            productService.insertProduct(product);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertFalse("Security Vulnerability Detected: SQLi insert-base",e.getMessage().contains("Syntax error in SQL statement"));
        }

    }

    @Test
    public void testSelect() {
        try {
            productService.selectProductByName("Laptop'");
        } catch (Exception e) {
            Assert.assertFalse("Security Vulnerability Detected: SQLi select-base",e.getMessage().contains("Syntax error in SQL statement"));
        }
    }

    @Test
    public void testUpdate() {
        Product product = new Product();
        product.setId(3);
        product.setName("Laptop'");
        try {
            productService.selectProductByName("Laptop'");
        } catch (Exception e) {
            Assert.assertFalse("Security Vulnerability Detected: SQLi update-base",e.getMessage().contains("Syntax error in SQL statement"));
        }
    }

    @Test
    public void testDelete() {
        try {
            productService.deleteProduct("Laptop'");
        } catch (Exception e) {
            Assert.assertFalse("Security Vulnerability Detected: SQLi delete-base",e.getMessage().contains("Syntax error in SQL statement"));
        }
    }
}