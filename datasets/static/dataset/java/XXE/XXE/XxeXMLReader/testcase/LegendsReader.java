<filename>src/main/java/legends/LegendsReader.java<fim_prefix>

package legends;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import legends.model.LegendsXml;
import legends.xml.handlers.AnnotationContentHandler;

public class LegendsReader {
    private static final Log LOG = LogFactory.getLog(LegendsReader.class);

    public static void read(Path path, Charset cs) throws SAXException, IOException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        LOG.info("load legends: " + path);
        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        <fim_suffix>
        AnnotationContentHandler contentHandler = new AnnotationContentHandler(LegendsXml.class);
        contentHandler.setXmlReader(xmlReader);
        xmlReader.setContentHandler(contentHandler);

        CharsetDecoder decoder = cs.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.IGNORE);

        BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(path), decoder));

        InputSource inputSource = new InputSource(reader);
        xmlReader.parse(inputSource);

        contentHandler.printMappedValues();
    }
}

<fim_middle>