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

package com.example.test;

import com.example.service.ReportService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Random;

/**
 * Security tests for the ReportService.
 * These tests attempt to exploit potential vulnerabilities and assert that the exploitation fails.
 */
public class SecurityTest {

    private ReportService reportService;
    private File tempFile;
    private String secretContent;

    /**
     * Sets up the test environment by creating a temporary file with secret content.
     * This file will be the target of the XXE attack.
     */
    @Before
    public void setUp() throws IOException {
        reportService = new ReportService();
        
        // 1. Create a temporary file to be read by the XXE payload
        int randomInt = new Random().nextInt(100000);
        secretContent = "secret_data_" + randomInt;
        
        tempFile = File.createTempFile("secret_" + randomInt, ".txt");

        // 2. Write the secret content to the file
        try (Writer writer = new FileWriter(tempFile)) {
            writer.write(secretContent);
        }
    }

    /**
     * Cleans up the test environment by deleting the temporary file.
     */
    @After
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    /**
     * Tests for XXE (XML External Entity) vulnerability.
     * This test constructs a malicious XML payload that attempts to read a local file.
     * The test PASSES if the file content is NOT returned (i.e., the attack is unsuccessful).
     * The test FAILS if the file content IS returned (i.e., the application is vulnerable).
     */
    @Test
    public void testExtractMetric_WithXxePayload() {
        // 1. Get the platform-independent URI of the temporary file
        String fileUri = tempFile.toURI().toString();

        // 2. Construct the XXE payload
        // The payload defines an external entity 'xxe' that points to our secret file.
        // It then tries to display the content of that entity within the 'leaked' tag.
        String xxePayload = "<?xml version=\"1.0\"?>" +
                "<!DOCTYPE data [" +
                "  <!ENTITY xxe SYSTEM \"" + fileUri + "\">" +
                "]>" +
                "<report>" +
                "  <data>" +
                "    <metric name=\"leaked\">&xxe;</metric>" +
                "  </data>" +
                "</report>";

        String xpathExpression = "/report/data/metric[@name='leaked']";

        // 3. Call the service with the malicious payload
        String result = null;
        try {
            result = reportService.extractMetric(xxePayload, xpathExpression);
        } catch (Exception e) {
            // Ignore the exception
        }
        if (result != null && result.contains(secretContent)) {
            // The service returned the secret content, indicating that the attack was successful
            Assert.fail("The service is vulnerable to XXE. The secret content should not be exposed.");
        }

    }
}