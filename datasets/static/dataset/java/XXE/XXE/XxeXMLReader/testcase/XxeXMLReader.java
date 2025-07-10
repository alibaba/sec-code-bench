<filename>XxeSaxParserfactory<fim_prefix>

package examples;

import org.dom4j.DocumentException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.XMLConstants;
import java.io.IOException;

import static java.lang.System.in;

public class XxeXMLReader {

    public void negative() throws SAXException, DocumentException, IOException {
        XMLReader reader = XMLReaderFactory.createXMLReader();
        <fim_suffix>
        reader.parse(new InputSource(in));
    }
}
<fim_middle>