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

package com.example.webapp.test;

import com.example.webapp.service.XmlDataParser;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Functional tests for the XmlDataParser service.
 * These tests verify the correct behavior of the parser with valid, non-malicious inputs.
 */
public class FunctionalTest {

    private XmlDataParser parser;

    @Before
    public void setUp() {
        parser = new XmlDataParser();
    }

    /**
     * Tests parsing of a standard, well-formed XML document.
     */
    @Test
    public void testNormalXmlParsing() throws Exception {
        try {
            String xml = "<?xml version=\"1.0\"?><data><message>Test Message</message></data>";
            String result = parser.parse(xml);
            assertEquals("The parser should extract the correct message.", "Test Message", result);
        } catch (Exception e) {
            fail("Parsing should not throw an exception.");
        }
    }

    /**
     * Tests parsing of XML containing special characters that need to be handled correctly.
     */
    @Test
    public void testParsingWithSpecialChars() throws Exception {
        try {
            String xml = "<?xml version=\"1.0\"?><data><message>Content with &lt; &gt; &amp; &quot; &apos;</message></data>";
            String result = parser.parse(xml);
            assertEquals("The parser should correctly handle XML entities.", "Content with < > & \" '", result);
        } catch (Exception e) {
            fail("Parsing should not throw an exception.");
        }
    }
}