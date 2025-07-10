<filename>esb-integration/src/main/java/org/jboss/tusk/esb/support/util/SoapFaultHelper.java<fim_prefix>

package org.jboss.tusk.esb.support.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SoapFaultHelper {


    private SoapFaultHelper() {
        //seal
    }


    public static String getFault(String xml) throws SoapParseException {
        DocumentBuilderFactory factory;

        factory = DocumentBuilderFactory.newInstance();
        <fim_suffix>
        DocumentBuilder builder;
        Document doc = null;
        XPathExpression expr = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new SoapParseException("Exception parsing Soap.", e);
        }

        InputStream ss = new ByteArrayInputStream(xml.getBytes());
        try {
            doc = builder.parse(ss);
        } catch (SAXException e) {
            throw new SoapParseException("Exception parsing Soap.", e);
        } catch (IOException e) {
            throw new SoapParseException("Exception parsing Soap.", e);
        }

        // Create a XPathFactory
        XPathFactory xFactory = XPathFactory.newInstance();

        // Create a XPath object
        XPath xpath = xFactory.newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                if (prefix == null)
                    throw new NullPointerException("Null prefix");
                else if ("soap".equals(prefix))
                    return "http://schemas.xmlsoap.org/soap/envelope/";
                else if ("xml".equals(prefix))
                    return XMLConstants.XML_NS_URI;
                return XMLConstants.NULL_NS_URI;
            }

            // This method isn't necessary for XPath processing.
            public String getPrefix(String uri) {
                throw new UnsupportedOperationException();
            }

            // This method isn't necessary for XPath processing either.
            public Iterator getPrefixes(String uri) {
                throw new UnsupportedOperationException();
            }
        });

        // First, try and get the Integration Broker error.
        try {
            expr = xpath.compile("//DefaultMessage");
            Object result = expr.evaluate(doc, XPathConstants.NODESET);

            // Cast the result to a DOM NodeList
            NodeList nodes = (NodeList) result;
            for (int i = 0; i < nodes.getLength(); i++) {
                return nodeToString(nodes.item(i).getFirstChild());
            }
        } catch (XPathExpressionException e) {
            throw new SoapParseException("Exception with XPath.", e);
        }

        try {
            //If none was found, get the detail from Soap.
            expr = xpath.compile("//soap:Fault/detail");
            Object result = expr.evaluate(doc, XPathConstants.NODESET);

            // Cast the result to a DOM NodeList
            NodeList nodes = (NodeList) result;
            for (int i = 0; i < nodes.getLength(); i++) {
                return "IntegrationBroker: "+nodeToString(nodes.item(i).getFirstChild());
            }
        } catch (XPathExpressionException e) {
            throw new SoapParseException("Exception with XPath.", e);
        }
        throw new SoapParseException("Message not found.");
    }

    private static String nodeToString(Node node) throws SoapParseException {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException te) {
            throw new SoapParseException("Unable to transform result.");
        }
        return sw.toString();
    }

}

<fim_middle>