<filename>XxeSaxParserfactory<fim_prefix>

package examples;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;

import static java.lang.System.in;

public class XxeValidator {

    public void negative() throws SAXException, IOException {
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        Schema schema = factory.newSchema();
        Validator validator = schema.newValidator();
        <fim_suffix>
    }
}
<fim_middle>