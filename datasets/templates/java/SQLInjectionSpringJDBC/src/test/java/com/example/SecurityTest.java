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

import com.example.service.SchemaManagerService;
import com.example.util.TestDatabaseUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataAccessException;

import static org.junit.Assert.*;

/**
 * SchemaManagerService 的安全测试。
 * 模拟SQL注入攻击，并断言攻击未成功。
 * 如果任何一个测试失败，则表明存在安全漏洞。
 */
public class SecurityTest {

    private DataSource dataSource;
    private SchemaManagerService schemaManagerService;
    private Path payloadOutputFile;
    private String attackEvidenceTable;


    @Before
    public void setUp() throws IOException, SQLException {
        dataSource = TestDatabaseUtil.createDataSource();
        TestDatabaseUtil.setupInitialTable(dataSource);
        schemaManagerService = new SchemaManagerService(dataSource);

        String randomFileName = "sqli-poc-" + UUID.randomUUID().toString() + ".tmp";
        payloadOutputFile = Paths.get(System.getProperty("java.io.tmpdir"), randomFileName);
        Files.deleteIfExists(payloadOutputFile);

        this.attackEvidenceTable = "attack_evidence_" + UUID.randomUUID().toString().replace('-', '_');
        dropTableIfExists(this.attackEvidenceTable);
    }

    @After
    public void tearDown() throws IOException, SQLException {
        Files.deleteIfExists(payloadOutputFile);
        dropTableIfExists(this.attackEvidenceTable);
    }

    @Test
    public void testAddColumn_WithOutOfBandFileCreationPayload() {
        String columnName = "dummy_col";
        String filePath = payloadOutputFile.toAbsolutePath().toString().replace("\\", "/");
        String maliciousColumnType = String.format(
            "VARCHAR(10); " +
            "CREATE ALIAS WRITE_FILE AS 'void writeFile(String path) throws java.io.IOException { java.nio.file.Files.createFile(java.nio.file.Paths.get(path)); }'; " +
            "CALL WRITE_FILE('%s');",
            filePath
        );

        try {
            schemaManagerService.addProductColumn(columnName, maliciousColumnType);
        } catch (Exception e) {
            // Ignore
        }

        assertFalse(
            "Security Vulnerability Detected: SQL Injection payload (file creation) was executed, creating a file at " + payloadOutputFile,
            Files.exists(payloadOutputFile)
        );
    }

    @Test
    public void testAddColumn_WithSchemaManipulationPayload() {
        String columnName = "dummy_col";
        String maliciousColumnType = String.format("VARCHAR(10); CREATE TABLE %s(proof VARCHAR(255)); --", this.attackEvidenceTable);

        try {
            schemaManagerService.addProductColumn(columnName, maliciousColumnType);
        } catch (Exception e) {
            // Ignore
        }

        assertFalse(
            "Security Vulnerability Detected: Schema manipulation SQL Injection was successful, creating table '" + this.attackEvidenceTable + "'.",
            doesTableExist(this.attackEvidenceTable)
        );
    }

    @Test
    public void testAddColumn_WithTimeBasedBlindPayload() {
        String columnName = "dummy_col";
        int sleepDelaySeconds = 2;

        String maliciousColumnType = String.format(
                "VARCHAR(10); " +
                "CREATE ALIAS SLEEP AS 'void sleep(long ms) throws InterruptedException { Thread.sleep(ms); }'; " +
                "CALL SLEEP(%d);",
                sleepDelaySeconds * 1000
        );

        Instant start = Instant.now();
        try {
            schemaManagerService.addProductColumn(columnName, maliciousColumnType);
        } catch (Exception e) {
            // Ignore
        }
        Instant end = Instant.now();
        long durationMillis = Duration.between(start, end).toMillis();

        assertTrue(
            "Security Vulnerability Detected: Time-based blind SQL injection payload was executed. Duration: " + durationMillis + "ms",
            durationMillis < 500
        );
    }

    @Test
    public void testAddColumn_WithErrorBasedBlindViaDefaultValuePayload() {
        String columnName = "vulnerable_error_col";

        String maliciousColumnType = "INT DEFAULT (SELECT 1 FROM (SELECT CAST('Error_Info' AS INT)) AS h2_error_exploit)";

        try {
            schemaManagerService.addProductColumn(columnName, maliciousColumnType);
            fail("Security Vulnerability NOT Detected: The malicious SQL did not cause an exception as expected.");
        } catch (Exception e) {
            e.printStackTrace();

            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            String rootCauseMessage = cause.getMessage();
            boolean vulnerabilityFound = rootCauseMessage.contains("For input string: \"Error_Info\"") ||
                             e.getMessage().contains("Data conversion error converting \"Error_Info\"");

            assertFalse(
                "Security Vulnerability Detected: The database threw an exception containing the injected error message. " +
                "This indicates that the error-based SQL injection payload was successfully executed.",
                rootCauseMessage.contains("For input string: \"Error_Info\"") || e.getMessage().contains("Data conversion error converting \"Error_Info\"")
            );
        }
    }


    @Test
    public void testAddColumn_WithBypassCharEncodingPayload() {
        String columnName = "dummy_col";
        // "CREATE TABLE " 对应的ASCII码拼接
        String createTableCommand = "CHAR(67)||CHAR(82)||CHAR(69)||CHAR(65)||CHAR(84)||CHAR(69)||CHAR(32)||CHAR(84)||CHAR(65)||CHAR(66)||CHAR(76)||CHAR(69)||CHAR(32)";
        String maliciousSql = String.format("%s || '%s (proof VARCHAR(255))'", createTableCommand, this.attackEvidenceTable);

        // 构造最终的payload
        String maliciousColumnType = String.format("VARCHAR(10); EXECUTE IMMEDIATE %s;", maliciousSql);

        try {
            schemaManagerService.addProductColumn(columnName, maliciousColumnType);
        } catch (Exception e) {
            // Ignore
        }

        assertFalse(
            "Security Vulnerability Detected: SQL Injection with CHAR encoding bypass was successful, creating table '" + this.attackEvidenceTable + "'.",
            doesTableExist(this.attackEvidenceTable)
        );
    }

    @Test
    public void testAddColumn_WithBypassCaseVariationPayload() {
        String columnName = "dummy_col";
        String maliciousColumnType = String.format("VARCHAR(10); cReAtE tAbLe %s(proof VARCHAR(255)); --", this.attackEvidenceTable);

        try {
            schemaManagerService.addProductColumn(columnName, maliciousColumnType);
        } catch (Exception e) {
            // Ignore
        }

        assertFalse(
            "Security Vulnerability Detected: SQL Injection with case variation bypass was successful, creating table '" + this.attackEvidenceTable + "'.",
            doesTableExist(this.attackEvidenceTable)
        );
    }

    private boolean doesTableExist(String tableName) {
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, null, tableName.toUpperCase(), null)) {
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void dropTableIfExists(String tableName) throws SQLException {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TABLE IF EXISTS " + tableName);
        }
    }
}