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
package com.example.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * 销售数据模型类
 * 对应数据库表 sales_data
 */
public class Sale {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 地区
     */
    private String region;
    
    /**
     * 产品分类
     */
    private String productCategory;
    
    /**
     * 销售员
     */
    private String salesPerson;
    
    /**
     * 销售金额
     */
    private BigDecimal salesAmount;
    
    /**
     * 销售日期
     */
    private Date saleDate;
    
    /**
     * 创建时间
     */
    private Timestamp createdTime;
    
    // 无参构造函数
    public Sale() {}
    
    // 全参构造函数
    public Sale(Long id, String region, String productCategory, String salesPerson, 
                BigDecimal salesAmount, Date saleDate, Timestamp createdTime) {
        this.id = id;
        this.region = region;
        this.productCategory = productCategory;
        this.salesPerson = salesPerson;
        this.salesAmount = salesAmount;
        this.saleDate = saleDate;
        this.createdTime = createdTime;
    }
    
    // Getter 和 Setter 方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    public String getProductCategory() {
        return productCategory;
    }
    
    public void setProductCategory(String productCategory) {
        this.productCategory = productCategory;
    }
    
    public String getSalesPerson() {
        return salesPerson;
    }
    
    public void setSalesPerson(String salesPerson) {
        this.salesPerson = salesPerson;
    }
    
    public BigDecimal getSalesAmount() {
        return salesAmount;
    }
    
    public void setSalesAmount(BigDecimal salesAmount) {
        this.salesAmount = salesAmount;
    }
    
    public Date getSaleDate() {
        return saleDate;
    }
    
    public void setSaleDate(Date saleDate) {
        this.saleDate = saleDate;
    }
    
    public Timestamp getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(Timestamp createdTime) {
        this.createdTime = createdTime;
    }
    
    @Override
    public String toString() {
        return "Sale{" +
                "id=" + id +
                ", region='" + region + '\'' +
                ", productCategory='" + productCategory + '\'' +
                ", salesPerson='" + salesPerson + '\'' +
                ", salesAmount=" + salesAmount +
                ", saleDate=" + saleDate +
                ", createdTime=" + createdTime +
                '}';
    }
} 