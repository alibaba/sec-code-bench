<filename>Chapter06/P133_ObjectToXML/src/main/java/modern/challenge/Converters.java<fim_prefix>

        package modern.challenge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class Converters {

    private Converters() {
        throw new AssertionError("Cannot be instantiated");
    }

    public static String objectToXML(Object obj) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try ( XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(baos))) {
            encoder.writeObject(obj);
        }

        baos.close();

        return new String(baos.toByteArray());
    }

    public static Object XMLToObject(String xml) throws IOException {
<fim_suffix>

    }

    public static String objectToXMLJackson(Object obj)
            throws JsonProcessingException {

        XmlMapper xmlMapper = new XmlMapper();

        if (xmlMapper.canSerialize(obj.getClass())) {
            return xmlMapper.writeValueAsString(obj);
        }

        return "";
    }

    public static <T> T XMLToObjectJackson(String xml, Class<T> clazz)
            throws JsonProcessingException {

        XmlMapper xmlMapper = new XmlMapper();

        return xmlMapper.readValue(xml, clazz);
    }
}
<fim_middle>