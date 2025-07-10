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

import com.example.User;
import com.example.UserMapper;
import com.example.UserService;
import com.example.UserServiceImpl;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import javax.sql.DataSource;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityTest {

    private SqlSessionFactory sqlSessionFactory;
    private DataSource dataSource = createH2DataSource();
    @BeforeAll
    void setup() throws Exception {
        Environment environment = new Environment(
                "test",
                new JdbcTransactionFactory(),
                dataSource
        );

        Configuration config = new Configuration(environment);

        config.addMapper(UserMapper.class);

        sqlSessionFactory = new SqlSessionFactoryBuilder().build(config);

        initializeDatabase(dataSource);
    }

    @AfterEach
    void rollbackTransaction() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            session.rollback();
        }
    }

    @Timeout(value = 10)
    @DisplayName("Security Test Case")
    @ParameterizedTest(name = "{index} => Case: {0}")
    @ArgumentsSource(SecurityArgumentsProvider.class)
    void securityTest(String caseName, Map input, Map verifyInfo) {

        String sql = (String) input.get("sql");
        try{
            initializeDatabaseBySql(dataSource, sql);
        }catch (Exception e){
            e.printStackTrace();
        }

        String order = (String) input.get("order");
        String sort = (String) input.get("sort");
        int limit = ((Double) input.get("limit")).intValue();
        int unExpectedCount = ((Double) verifyInfo.get("inject_limit")).intValue();
        try (SqlSession session = sqlSessionFactory.openSession()) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            UserService service = new UserServiceImpl(mapper);
            List<User> users = service.listUser(sort, order,  limit);
            assertNotEquals(unExpectedCount, users.size(), "SQL Injection");
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    private DataSource createH2DataSource() {
        PooledDataSource dataSource = new PooledDataSource();
        dataSource.setDriver("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private void initializeDatabase(DataSource dataSource) throws Exception {
        try (Connection conn = dataSource.getConnection();
             InputStream inputStream = getClass().getClassLoader().getResourceAsStream("init-db.sql");
             InputStreamReader reader = new InputStreamReader(inputStream)) {

            ScriptRunner runner = new ScriptRunner(conn);
            runner.setAutoCommit(false);
            runner.setStopOnError(true);
            runner.setLogWriter(null);
            runner.runScript(reader);
        }
    }

    private void initializeDatabaseBySql(DataSource dataSource, String sql) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            Reader reader = new StringReader(sql);
            ScriptRunner runner = new ScriptRunner(conn);
            runner.setAutoCommit(false);
            runner.setStopOnError(true);
            runner.setLogWriter(null);
            runner.runScript(reader);
        }
    }
}