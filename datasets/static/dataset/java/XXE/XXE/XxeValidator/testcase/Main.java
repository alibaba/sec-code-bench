<filename>JavaEE/JavaWebServices/JAXB/XMLValidator/src/main/java/ar/com/javacuriosities/xml/validator/Main.java<fim_prefix>

package ar.com.javacuriosities.xml.validator;


import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class Main {

    private static final String SCHOOL_SCHEMA = "school.xsd";
    private static final String SCHOOL_DOCUMENT = "school.xml";

    public static void main(String[] args) {
        domValidation();
        streamValidation();
    }

    public static void domValidation() {
        try {
            // Debemos crear y configurar la factory de parsers de documentos XML
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // dbf.setNamespaceAware(true);  // Activamos soporte para namespaces

            // Cargamos el documento XML
            DocumentBuilder parser = dbf.newDocumentBuilder();

            Document doc = parser.parse(new File(ClassLoader.getSystemResource(SCHOOL_DOCUMENT).toURI()));

            // Creamos una SchemaFactory preparada para interpretar esquemas XML W3C
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            // Cargamos el esquema XSD
            Schema schema = sf.newSchema(new File(ClassLoader.getSystemResource(SCHOOL_SCHEMA).toURI()));

            // Creamos el objeto validator, que será el responsable de validar el XML
            Validator validator = schema.newValidator();

            <fim_suffix>
            validator.validate(new DOMSource(doc));

            // Si se llega a este punto, el documento es válido
            System.out.println("DOM Validation Complete");
        } catch (SAXException | IOException | ParserConfigurationException | URISyntaxException e) {
            // Log and Handle exception
            e.printStackTrace();
        }
    }

}

<fim_middle>