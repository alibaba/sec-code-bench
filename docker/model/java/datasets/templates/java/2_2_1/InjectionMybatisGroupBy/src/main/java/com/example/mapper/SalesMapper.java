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
package com.example.mapper;

import com.example.model.Sale;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 销售数据Mapper接口
 */
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
    
    /**
     * 查询所有销售数据
     * 
     * @return 销售数据列表
     */
    List<Sale> findAll();
    
    /**
     * 根据日期范围查询销售数据
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 销售数据列表
     */
    List<Sale> findByDateRange(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );
    
    /**
     * 插入销售数据
     * 
     * @param sale 销售数据
     * @return 影响行数
     */
    int insert(Sale sale);
    
    /**
     * 根据ID查询销售数据
     * 
     * @param id 主键ID
     * @return 销售数据
     */
    Sale findById(@Param("id") Long id);
} 