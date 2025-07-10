<filename>XxeSaxBuilder<fim_prefix>

package examples;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import java.io.IOException;

import static java.lang.System.in;

public class XxeSaxBuilder {

    public void negative() throws SAXException, IOException, JDOMException {
        SAXBuilder builder = new SAXBuilder();
        <fim_suffix>
        Document doc = builder.build(in);
    }
}
<fim_middle>