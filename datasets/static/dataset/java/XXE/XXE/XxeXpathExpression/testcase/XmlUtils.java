<filename>src/main/java/com/github/terma/jenkins/githubprcoveragestatus/XmlUtils.java<fim_prefix>

/*

    Copyright 2015-2016 Artem Stasiuk

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/
        package com.github.terma.jenkins.githubprcoveragestatus;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

public class XmlUtils {

    public static String findInXml(String xml, String xpath) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            <fim_suffix>
            builder.setEntityResolver(new EntityResolver() {
                @Override
                public InputSource resolveEntity(String publicId, String systemId)
                        throws SAXException, IOException {
//                    if (systemId.contains("foo.dtd")) {
                    return new InputSource(new StringReader(""));
//                    } else {
//                        return null;
//                    }
                }
            });
            Document doc = builder.parse(new InputSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPathExpression expr = xPathfactory.newXPath().compile(xpath);
            return (String) expr.evaluate(doc, XPathConstants.STRING);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

}

<fim_middle>