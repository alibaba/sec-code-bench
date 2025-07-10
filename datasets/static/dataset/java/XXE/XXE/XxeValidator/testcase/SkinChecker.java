<filename>src/org/open2jam/render/SkinChecker.java<fim_prefix>

package org.open2jam.render;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.open2jam.util.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


public class SkinChecker {

    String schemaLocation = "/resources/skin_schema.xsd";

    class SimpleErrorHandler implements ErrorHandler {
        @Override
        public void warning(SAXParseException exception) throws SAXException {
            Logger.global.log(Level.WARNING, "Warning [line:{2} column:{1}] : {0}", new Object[]{exception.getMessage(), exception.getColumnNumber(), exception.getLineNumber()});
        }
        @Override
        public void error(SAXParseException exception) throws SAXException {
            Logger.global.log(Level.SEVERE, "Error [line:{2} column:{1}] : {0}", new Object[]{exception.getMessage(), exception.getColumnNumber(), exception.getLineNumber()});
        }
        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            Logger.global.log(Level.SEVERE, "FatalError [line:{2} column:{1}] : {0}", new Object[]{exception.getMessage(), exception.getColumnNumber(), exception.getLineNumber()});
            throw exception;
        }
    }

    public boolean validate(String xmlFile) throws SAXException, IOException {

        InputStream input_stream = null;
        URL url = SkinChecker.class.getResource(xmlFile);
        if(url == null)
        {
            File file = new File(xmlFile);
            if(!file.exists())
            {
                Logger.global.log(Level.SEVERE, "There is no xml file {0}", xmlFile);
                return false;
            }
            input_stream = new FileInputStream(file);
        }
        else
        {
            input_stream = url.openStream();
        }

        if(input_stream == null)
            Logger.global.log(Level.SEVERE, "There is no xml file {0}", xmlFile);

        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

        URL schema_file = SkinChecker.class.getResource(schemaLocation);
        Schema schema = factory.newSchema(schema_file);

        Validator validator = schema.newValidator();

        Source source = new StreamSource(input_stream);

        ErrorHandler error_handler = new SimpleErrorHandler();
        <fim_suffix>

        try {
            validator.validate(source);
            return true; //it's valid.
        }
        catch (SAXException ex) {
            return false;
        }
    }
}

<fim_middle>