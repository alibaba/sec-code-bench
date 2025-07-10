<filename>XxeSaxParserfactory<fim_prefix>

package examples;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.StringReader;

public class XxeUnmarshaller {

    public void negative(String xml) throws SAXException, ParserConfigurationException, JAXBException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        <fim_suffix>
        um.unmarshal(xmlSource);
    }
}
<fim_middle>