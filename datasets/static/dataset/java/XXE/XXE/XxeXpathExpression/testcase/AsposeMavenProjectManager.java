<filename>Plugins/Aspose_Slides_Java_Maven_for_Eclipse/AsposeSlidesEclipsePlugin/src/com/aspose/slides/maven/utils/AsposeMavenProjectManager.java<fim_prefix>

/*
 * The MIT License (MIT)
 *
 * Copyright (c) 1998-2016 Aspose Pty Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.aspose.slides.maven.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.aspose.slides.maven.artifacts.Metadata;

public class AsposeMavenProjectManager {

    private File projectDir = null;

    private static final List<Metadata> asposeProjectMavenDependencies = new ArrayList<Metadata>();

    /**
     *
     * @return
     */
    public static List<Metadata> getAsposeProjectMavenDependencies() {
        return asposeProjectMavenDependencies;
    }

    /**
     *
     */
    public static void clearAsposeProjectMavenDependencies() {
        asposeProjectMavenDependencies.clear();
    }

    /**
     *
     * @return
     */
    public File getProjectDir() {
        return projectDir;
    }

    public String getDependencyVersionFromPOM(URI projectDir, String dependencyName) {
        try {
            String mavenPomXmlfile = projectDir.getPath() + File.separator + AsposeConstants.MAVEN_POM_XML;

            if (new File(mavenPomXmlfile).exists()) {
                Document pomDocument = getXmlDocument(mavenPomXmlfile);

                XPathFactory xPathfactory = XPathFactory.newInstance();
                XPath xpath = xPathfactory.newXPath();
                String expression = "//version[ancestor::dependency/artifactId[text()='" + dependencyName + "']]";
                XPathExpression xPathExpr = xpath.compile(expression);
                NodeList nl = (NodeList) xPathExpr.evaluate(pomDocument, XPathConstants.NODESET);

                if (nl != null && nl.getLength() > 0) {
                    return nl.item(0).getTextContent();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private Document getXmlDocument(String mavenPomXmlfile)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        <fim_suffix>
        Document pomDocument = (Document) docBuilder.parse(mavenPomXmlfile);

        return pomDocument;
    }

}
<fim_middle>