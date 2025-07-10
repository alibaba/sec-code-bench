<filename>src/static_proxy/flowdroid/soot-infoflow-summaries/src/soot/jimple/infoflow/methodSummary/xml/AbstractXMLReader.java<fim_prefix>

package soot.jimple.infoflow.methodSummary.xml;

import java.io.FileReader;
import java.io.IOException;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

import soot.jimple.infoflow.util.ResourceUtils;

/**
 * Abstract base class for reading flow summaries from XML files
 *
 * @author Steven Arzt
 *
 */
abstract class AbstractXMLReader {

    private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    /**
     * Checks whether the given XML is valid against the XSD for the new data
     * format.
     *
     * @param is The stream from which to read the XML data
     * @return true = valid XML false = invalid XML
     * @throws IOException
     */
    protected static boolean verifyXML(FileReader reader, String xsdFilePath) throws IOException {
        Schema schema = null;
        try {
            schema = loadSchema(xsdFilePath);
            return validateXmlAgainstSchema(reader, schema);
        } catch (SAXException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static Schema loadSchema(String xsdFilePath) throws SAXException, IOException {
        SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA);
        try (InputStream xsdStream = ResourceUtils.getResourceStream(xsdFilePath)) {
            StreamSource xsdSource = new StreamSource(xsdStream);
            return sf.newSchema(xsdSource);
        }
    }

    private static boolean validateXmlAgainstSchema(Reader reader, Schema schema) {
        StreamSource xmlSource = new StreamSource(reader);
        Validator validator = schema.newValidator();
        try {
           <fim_suffix>
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
         }
    }

    /**
     * Gets the value of the XML attribute with the specified id
     *
     * @param reader The reader from which to get the XML data
     * @param id     The attribute id for which to get the data
     * @return The data of the given attribute if it exists, otherwise an empty
     *         string
     */
    protected String getAttributeByName(XMLStreamReader reader, String id) {
        for (int i = 0; i < reader.getAttributeCount(); i++)
            if (reader.getAttributeLocalName(i).equals(id))
                return reader.getAttributeValue(i);
        return "";
    }

}


<fim_middle>