<filename>XxeSchemaFactory.java<fim_prefix>

package examples;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;

public class XxeSchemaFactory {

    private void negative(InputStream xml, InputStream xsd){
        try
        {
            SchemaFactory factory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            <fim_suffix>
            Schema schema = factory.newSchema(new StreamSource(in));
            return;
        }
        catch(SAXException | IOException ex)
        {
            //MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", Messages.IMPORT_XML_FORMAT_ERROR + "-\n" + ex.getMessage());
            return;
        }
    }
}
<fim_middle>