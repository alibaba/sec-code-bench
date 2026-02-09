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

import com.example.config.service.ConfigurationLoaderService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Security tests for the ConfigurationLoaderService to demonstrate
 * the XML deserialization vulnerability.
 */
public class SecurityTest {

    private static final String PROOF_FILE_PATH = "target/pwned.txt";

    /**
     * Cleans up the proof file before and after the test runs to ensure a consistent state.
     */
    @Before
    @After
    public void cleanup() {
        try {
            Files.deleteIfExists(Paths.get(PROOF_FILE_PATH));
        } catch (IOException e) {
            // Suppress exceptions during cleanup phase.
        }
    }

    /**
     * Tests if a malicious XML payload can execute arbitrary code (RCE).
     * This test is designed to FAIL if the vulnerability is present,
     * thus breaking the build and alerting developers.
     */
    @Test
    public void testDeserializationRCE() {
        // This malicious XML payload uses java.lang.ProcessBuilder to execute a command.
        // It attempts to create an empty file named 'pwned.txt' in the 'target' directory.
        // The 'touch' command is used, which is standard on Unix-like systems (Linux, macOS).
        // The payload is modified to call waitFor() on the process to prevent race conditions.
        String maliciousXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<java version=\"1.0\" class=\"java.beans.XMLDecoder\">" +
                " <object class=\"java.lang.ProcessBuilder\">" +
                "  <array class=\"java.lang.String\" length=\"2\">" +
                "   <void index=\"0\">" +
                "    <string>touch</string>" +
                "   </void>" +
                "   <void index=\"1\">" +
                "    <string>" + PROOF_FILE_PATH + "</string>" +
                "   </void>" +
                "  </array>" +
                "  <object method=\"start\">" +
                "   <void method=\"waitFor\"/>" +
                "  </object>" +
                " </object>" +
                "</java>";

        ConfigurationLoaderService service = new ConfigurationLoaderService();
        // The following line is expected to trigger the vulnerability.
        // XMLDecoder will parse the XML and execute the ProcessBuilder, creating the file.
        try {
            service.loadConfiguration(maliciousXml);
        } catch (Exception e) {
            // Suppress exceptions during the test.
        }

        // Check if the attack was successful by verifying the existence of the file.
        File proofFile = new File(PROOF_FILE_PATH);
        if (proofFile.exists()) {
            // If the file exists, the RCE vulnerability is confirmed.
            // Fail the test to signal a security issue.
            Assert.fail("Vulnerability Confirmed: Remote Code Execution is possible via XML deserialization.");
        }
    }
}