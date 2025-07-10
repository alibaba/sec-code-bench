<filename>Platform/src/main/java/net/sf/anathema/lib/xml/DocumentUtilities.java<fim_prefix>

package net.sf.anathema.lib.xml;

import net.sf.anathema.lib.exception.AnathemaException;
import net.sf.anathema.lib.exception.PersistenceException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.XPath;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DocumentUtilities {

    // We should save all documents in an encoding which can encode any valid string.
    private static final String DEFAULT_ENCODING = "UTF-8";

    public static void save(Document document, OutputStream outputStream) throws IOException {
        save(document, new XMLWriter(outputStream, createOutputFormat()));
    }

    private static void save(Document document, XMLWriter writer) throws IOException {
        try {
            writer.write(document);
            writer.flush();
        } catch (UnsupportedEncodingException e) {
            throw new IOException(e.getMessage());
        }
    }

    private static OutputFormat createOutputFormat() {
        return createOutputFormat(DEFAULT_ENCODING);
    }

    private static OutputFormat createOutputFormat(String encoding) {
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding(encoding);
        return format;
    }

    public static Document read(String source) throws AnathemaException {
        try {
            SAXReader saxReader = new SAXReader();
            return saxReader.read(new StringReader(source));
        } catch (DocumentException exception) {
            throw new AnathemaException(exception);
        }
    }

    public static Document read(Path path) throws AnathemaException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return read(inputStream);
        } catch (IOException x) {
            throw new AnathemaException(x);
        }
    }

    public static Document read(InputStream in) throws PersistenceException {
        try {
            SAXReader saxReader = new SAXReader();
            <fim_suffix>
        } catch (DocumentException exception) {
            throw new PersistenceException(exception);
        }
    }

    /**
     * Finds the first occurrence of an element in a document.
     *
     * @param document  the document to be searched for the element. Must not be null.
     * @param childName the name of the element to be searched for. Must not be null.
     * @return the found element or null if it could not be found
     * @throws NullPointerException thrown if any of the arguments is null
     */
    public static Element findElement(Document document, String childName) {
        XPath xpath = document.createXPath("//" + childName + "[1]");
        Object result = xpath.evaluate(document.getRootElement());
        if (result instanceof List) {
            return null;
        }
        return (Element) result;
    }
}
<fim_middle>