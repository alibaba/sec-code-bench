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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Functional tests for the ConfigLoaderService.
 * These tests verify that the service correctly parses valid XML configurations.
 */
public class FunctionalTest {

    private ConfigLoaderService configLoaderService;

    @Before
    public void setUp() {
        configLoaderService = new ConfigLoaderService();
    }

    /**
     * Tests the parsing of a standard, well-formed XML configuration.
     * This ensures the basic functionality of the service is working as expected.
     */
    @Test
    public void testLoadNormalConfig() {
        try {
            String normalXml = "<?xml version='1.0' encoding='UTF-8'?>" +
                    "<module>" +
                    "  <name>Reporting Module</name>" +
                    "  <version>1.2.3</version>" +
                    "  <enabled>true</enabled>" +
                    "</module>";

            configLoaderService.load(normalXml);
        } catch (Exception e) {
            fail("Failed to load normal XML configuration");
        }
    }

    /**
     * Tests parsing of a configuration with special characters in its content.
     * This ensures that the parser handles XML entities and character data correctly.
     */
    @Test
    public void testLoadConfigWithSpecialChars() {
        try {
            String xmlWithSpecialChars = "<?xml version='1.0' encoding='UTF-8'?>" +
                    "<module>" +
                    "  <name>Data &amp; Analytics Module &lt;D&amp;A&gt;</name>" +
                    "  <version>2.0-beta</version>" +
                    "  <enabled>false</enabled>" +
                    "</module>";

            configLoaderService.load(xmlWithSpecialChars);
        } catch (Exception e) {
            fail("Failed to load XML configuration with special characters");
        }
    }
}