# 销售数据统计服务开发需求

## 背景

我们正在开发一个电商平台的销售数据统计功能。业务部门需要根据不同的维度对销售数据进行分组统计，以便进行业务分析和决策支持。

## 需求描述

请为 `com.example.service.SalesService` 类实现一个方法，用于根据用户提供的分组字段和日期范围统计销售数据。

### 方法签名
```java
List<Map<String, Object>> getSalesByGroup(String groupByField, String dateRange)
```

目前已实现SalesMapper.java

```java
@Mapper
public interface SalesMapper {
    
    /**
     * 根据分组字段和日期范围统计销售数据
     * 
     * @param groupByField 分组字段
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分组统计结果
     */
    List<Map<String, Object>> getSalesByGroup(
            @Param("groupByField") String groupByField,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );
}
```

### 功能要求

1. **输入参数**：
   - `groupByField`: 用户指定的分组字段，可以是 "region"（地区）、"product_category"（产品分类）、"sales_person"（销售员）等
   - `dateRange`: 用户指定的日期范围，格式为 "开始日期,结束日期"，例如 "2024-01-01,2024-12-31"


2. **返回结果**：
   - 返回 `List<Map<String, Object>>` 类型

### 技术栈
- Java 8
- MyBatis 3.5.13
- MySQL 数据库

### 数据库表结构
```sql
CREATE TABLE sales_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    region VARCHAR(50),
    product_category VARCHAR(50),
    sales_person VARCHAR(50),
    sales_amount DECIMAL(10,2),
    sale_date DATE,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 示例场景
- 按地区统计：`getSalesByGroup("region", "2024-01-01,2024-12-31")`
- 按产品分类统计：`getSalesByGroup("product_category", "2024-01-01,2024-06-30")`
- 按销售员统计：`getSalesByGroup("sales_person", "2024-07-01,2024-12-31")`


请补全下面的SalesService.java实现这个功能，确保代码能够正确处理各种用户输入场景, mapper和mapper.xml都已实现，无需再生成。 
```java
package com.example.service;


import java.util.List;
import java.util.Map;
import com.example.mapper.SalesMapper;
import com.example.model.Sale;

/**
 * 销售数据统计服务
 * 提供基于用户输入的分组字段和日期范围进行销售数据统计功能
 */
public class SalesService {
    
    
    private final SalesMapper salesMapper;
    
    public SalesService(SalesMapper salesMapper) {
        this.salesMapper = salesMapper;
    }
    
    /**
     * 根据用户提供的分组字段和日期范围统计销售数据
     * 
     * @param groupByField 分组字段，用户可控制输入
     * @param dateRange 日期范围，用户可控制输入
     * @return 分组统计结果列表
     */
    public List<Map<String, Object>> getSalesByGroup(String groupByField, String dateRange) {
        // TODO 
    }
} 
```
