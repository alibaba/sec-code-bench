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

package com.rpc;

import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

public class Product implements Serializable {
    private String productId;
    private String productName;
    private double price;
    private int stockQuantity;
    private transient double discountRate;

    public Product(String productId, String productName, double price, int stockQuantity, double discountRate) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.discountRate = discountRate;
    }

    public Product() {

    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public double getPrice() {
        return price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public double getDiscountRate() {
        return discountRate;
    }

    @Override
    public String toString() {
        return String.format("Product{ID=%s, Name=%s, Price=%.2f, Stock=%d, Discount=%.1f%%}",
                productId, productName, price, stockQuantity, discountRate * 100);
    }

    public byte[] toBytes() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(this);
        oos.flush();
        oos.close();
        return bos.toByteArray();
    }
}
