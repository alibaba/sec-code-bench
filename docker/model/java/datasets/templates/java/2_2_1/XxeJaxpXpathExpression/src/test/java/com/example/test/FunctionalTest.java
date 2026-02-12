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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Functional tests for the ReportService.
 */
public class FunctionalTest {

    private ReportService reportService;

    @Before
    public void setUp() {
        reportService = new ReportService();
    }

    /**
     * Tests the metric extraction with a valid and normal XML input.
     * This ensures the core functionality works as expected.
     */
    @Test
    public void testExtractMetric_ValidInput() {
        try {
            String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<report>" +
                    "  <metadata>" +
                    "    <id>report-123</id>" +
                    "  </metadata>" +
                    "  <data>" +
                    "    <metric name=\"cpu_usage\">85.5</metric>" +
                    "    <metric name=\"memory_usage\">64.2</metric>" +
                    "  </data>" +
                    "</report>";

            String xpathExpression = "/report/data/metric[@name='cpu_usage']";
            String expected = "85.5";
            String actual = reportService.extractMetric(xmlContent, xpathExpression);

            Assert.assertEquals("The service should correctly extract the metric value.", expected, actual);
        } catch (Exception e) {
            Assert.fail("The service should not throw an exception.");
        }
    }
}