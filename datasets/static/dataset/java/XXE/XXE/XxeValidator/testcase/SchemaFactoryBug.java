<filename>library/deprecated/test/org/webpki/xml/SchemaFactoryBug.java<fim_prefix>

package org.webpki.xml;

import java.io.ByteArrayInputStream;

import java.util.ArrayList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class SchemaFactoryBug {
    static String xsd1 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<xs:schema targetNamespace=\"http://example.com/xmldsig11\"" +
                    "           xmlns:ds11=\"http://example.com/xmldsig11\"" +
                    "           xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"" +
                    "           elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">" +

                    "   <xs:element name=\"ECKeyValue\" type=\"xs:base64Binary\"/>" +

                    "</xs:schema>";

    static String xsd2 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<xs:schema targetNamespace=\"http://example.com/xmldsig\"" +
                    "           xmlns:ds=\"http://example.com/xmldsig\"" +
                    "           xmlns:ds11=\"http://example.com/xmldsig11\"" +
                    "           xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"" +
                    "           elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">" +

                    "   <xs:import namespace=\"http://example.com/xmldsig11\"/>" +

                    "   <xs:element name=\"KeyInfo\">" +
                    "      <xs:complexType>" +
                    "         <xs:sequence>" +
                    "            <xs:element ref=\"ds11:ECKeyValue\"/>" +
                    "         </xs:sequence>" +
                    "      </xs:complexType>" +
                    "   </xs:element>" +

                    "</xs:schema>";

    static String xsd3 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<xs:schema targetNamespace=\"http://example.com/xmldsig3\"" +
                    "           xmlns:ds=\"http://example.com/xmldsig3\"" +
                    "           xmlns:ds11=\"http://example.com/xmldsig11\"" +
                    "           xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"" +
                    "           elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">" +

                    "   <xs:import namespace=\"http://example.com/xmldsig11\"/>" +

                    "   <xs:element name=\"KeyInfo\">" +
                    "      <xs:complexType>" +
                    "         <xs:sequence>" +
                    "            <xs:element ref=\"ds11:ECKeyValue\"/>" +
                    "         </xs:sequence>" +
                    "      </xs:complexType>" +
                    "   </xs:element>" +

                    "</xs:schema>";

    static String xml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<key:KeyInfo xmlns:key=\"http://example.com/xmldsig\"" +
                    "             xmlns:ds11=\"http://example.com/xmldsig11\">" +
                    "    <ds11:ECKeyValue>AmA0R1CUdde3nakEJAFEqa29xtYQRaRXc7zB+iTOsV4=</ds11:ECKeyValue>" +
                    "</key:KeyInfo>";

    private static DOMSource getDOM(DocumentBuilder parser, String xml) throws Exception {
        return new DOMSource(parser.parse(new ByteArrayInputStream(xml.getBytes("UTF-8"))));
    }

    public static void main(String argv[]) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder parser = dbf.newDocumentBuilder();
            ArrayList<DOMSource> xsds = new ArrayList<>();
            xsds.add(getDOM(parser, xsd1));
            xsds.add(getDOM(parser, xsd2));
            xsds.add(getDOM(parser, xsd3));
            Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(xsds.toArray(new DOMSource[0]));
            Document document = parser.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
            Validator validator = schema.newValidator();
            <fim_suffix>
            validator.validate(getDOM(parser, xml));
            Element element = document.getDocumentElement();
            System.out.println("E=" + element.getLocalName() + " NS=" + element.lookupNamespaceURI(element.getPrefix()));
            schema = null;
            ArrayList<DOMSource> xsds2 = new ArrayList<>();
            xsds2.add(getDOM(parser, xsd1));
            xsds2.add(getDOM(parser, xsd2));
            Schema schema2 = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(xsds2.toArray(new DOMSource[0]));
            DocumentBuilderFactory dbf2 = DocumentBuilderFactory.newInstance();
            dbf2.setNamespaceAware(true);
            dbf2.setSchema(schema2);
            DocumentBuilder parser2 = dbf2.newDocumentBuilder();
            parser2.setErrorHandler(new ErrorHandler() {

                @Override
                public void warning(SAXParseException exception) throws SAXException {
                    throw new RuntimeException(exception);
                }

                @Override
                public void error(SAXParseException exception) throws SAXException {
                    throw new RuntimeException(exception);
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    throw new RuntimeException(exception);
                }
            });
            parser2.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

<fim_middle>