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

import com.example.service.SchemaValidationService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Functional tests for the SchemaValidationService.
 * These tests verify that the service correctly identifies valid and invalid schemas
 * under normal, non-malicious conditions.
 */
public class FunctionalTest {

    private SchemaValidationService schemaValidationService;

    @Before
    public void setUp() {
        schemaValidationService = new SchemaValidationService();
    }

    @Test
    public void testValidateSchema_WithValidAndWellFormedSchema_ShouldReturnTrue() {
        // This is a standard, simple, and valid W3C XML Schema.
        // It includes a comment to ensure special characters are handled correctly.
        String validSchema = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "  <!-- This is a comment -->\n" +
                "  <xs:element name=\"note\">\n" +
                "    <xs:complexType>\n" +
                "      <xs:sequence>\n" +
                "        <xs:element name=\"to\" type=\"xs:string\"/>\n" +
                "        <xs:element name=\"from\" type=\"xs:string\"/>\n" +
                "        <xs:element name=\"heading\" type=\"xs:string\"/>\n" +
                "        <xs:element name=\"body\" type=\"xs:string\"/>\n" +
                "      </xs:sequence>\n" +
                "    </xs:complexType>\n" +
                "  </xs:element>\n" +
                "</xs:schema>";

        boolean isValid = schemaValidationService.validateSchema(validSchema);
        assertTrue("Service failed to validate a correct schema.", isValid);
    }

    @Test
    public void testValidateSchema_WithMalformedSchema_ShouldReturnFalse() {
        // This schema is malformed because the <xs:element> tag is not closed.
        String malformedSchema = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "  <xs:element name=\"note\">\n" +
                "</xs:schema>";

        boolean isValid = schemaValidationService.validateSchema(malformedSchema);
        assertFalse("Service incorrectly validated a malformed schema.", isValid);
    }
}