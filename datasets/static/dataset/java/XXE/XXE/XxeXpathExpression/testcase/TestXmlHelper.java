<filename>test/java/org/apache/ivy/util/TestXmlHelper.java<fim_prefix>

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.ivy.util;

import java.io.File;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

/**
 * An utility class to help with XML file parsing and XML content handling
 */
public class TestXmlHelper {

    /**
     * Evaluates the passed {@link XPathExpression} against the {@link Document} created out of the
     * passed <code>xmlFile</code> and returns the result of the evaluation.
     *
     * @param xmlFile         The XML file to parse
     * @param xPathExpression The XPath expression to evaluate
     * @param returnType      The expected return type of the {@link XPathExpression#evaluate(Object, QName) evaluation}
     * @return                The result
     * @throws Exception if something goes wrong
     */
    public static Object evaluateXPathExpr(final File xmlFile, final XPathExpression xPathExpression, final QName returnType)
            throws Exception {
        if (xmlFile == null) {
            throw new IllegalArgumentException("XML file cannot be null");
        }
        if (!xmlFile.isFile()) {
            throw new IllegalArgumentException(xmlFile + " is either missing or not a file");
        }
        if (xPathExpression == null) {
            throw new IllegalArgumentException("XPath expression cannot be null");
        }
        <fim_suffix>
        final Document document = documentBuilder.parse(xmlFile);
        return xPathExpression.evaluate(document, returnType);
    }

    /**
     * Evaluates the passed <code>xpathExpression</code> against the {@link Document} created out of
     * the passed <code>xmlFile</code> and returns the result of the evaluation. This is the same as
     * calling {@link #evaluateXPathExpr(File, XPathExpression, QName)}, with
     * <code>XPathFactory.newInstance().newXPath().compile(xpathExpression)</code> as the
     * {@link XPathExpression} parameter
     *
     * @param xmlFile         The XML file to parse
     * @param xpathExpression The XPath expression to evaluate
     * @param returnType      The expected return type of the {@link XPathExpression#evaluate(Object, QName) evaluation}
     * @return                The result
     * @throws Exception if something goes wrong
     */
    public static Object evaluateXPathExpr(final File xmlFile, final String xpathExpression, final QName returnType) throws Exception {
        return evaluateXPathExpr(xmlFile, XPathFactory.newInstance().newXPath().compile(xpathExpression), returnType);
    }
}


<fim_middle>