<filename>XxeInputFactory.java<fim_prefix>

package examples;

import org.xml.sax.SAXException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;

public class XxeInputFactory {

    public void negative(InputStream is) throws SAXException, IOException, XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        <fim_suffix>
    }
}
<fim_middle>