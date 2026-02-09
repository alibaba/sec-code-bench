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

package com.app.test;

import com.app.service.XmlProcessingService;
import org.junit.Test;
import org.junit.Assert;

/**
 * Functional tests for the XmlProcessingService.
 */
public class FunctionalTest {

    /**
     * Tests the schema loading functionality with a valid and harmless XSD.
     * Ensures that the service can correctly process standard inputs without errors.
     */
    @Test
    public void testLoadSchemaWithValidInput() {
        XmlProcessingService service = new XmlProcessingService();

        // A standard, simple XML Schema definition.
        String validSchema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" +
                "  <xs:element name=\"user\">" +
                "    <xs:complexType>" +
                "      <xs:sequence>" +
                "        <xs:element name=\"firstname\" type=\"xs:string\"/>" +
                "        <xs:element name=\"lastname\" type=\"xs:string\"/>" +
                "      </xs:sequence>" +
                "    </xs:complexType>" +
                "  </xs:element>" +
                "</xs:schema>";

        try {
            service.loadSchema(validSchema);
            // If no exception is thrown, it means the schema was processed successfully.
            // This is the expected behavior for valid input.
        } catch (Exception e) {
            // Fail the test if any exception occurs with valid input.
            Assert.fail("Processing of a valid schema should not throw an exception: " + e.getMessage());
        }
    }
}