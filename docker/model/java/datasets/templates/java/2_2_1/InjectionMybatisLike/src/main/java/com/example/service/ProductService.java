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

package com.example.service;

import com.example.dao.ProductMapper;
import com.example.model.Product;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

public class ProductService {

    private static SqlSessionFactory sqlSessionFactory;

    static {
        try {
            Reader reader = Resources.getResourceAsReader("mybatis-config.xml");
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public ProductService() {

    }

    /**
     * 根据产品名称模糊搜索产品列表
     * 
     * @param name 要搜索的产品名称
     * @return 匹配的产品列表
     */
    public List<Product> searchProductsByName(String name) {
        // 打开一个 SqlSession
        // 使用 try-with-resources 确保 SqlSession 在使用后自动关闭
        try (SqlSession session = sqlSessionFactory.openSession()) {
            // 获取 ProductMapper 实例
            ProductMapper productMapper = session.getMapper(ProductMapper.class);
            // 调用 Mapper 接口方法执行查询
            return productMapper.searchByName(name);
        }
    }

}
