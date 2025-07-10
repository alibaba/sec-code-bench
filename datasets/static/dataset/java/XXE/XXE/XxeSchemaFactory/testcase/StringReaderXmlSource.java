<filename>src/main/java/com/setupmyproject/infra/StringReaderXmlSource.java<fim_prefix>

package com.setupmyproject.infra;

import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

/**
 * Implementacao que lê o xml a partir de uma string com o conteúdo.
 * @author alberto
 *
 */
public class StringReaderXmlSource implements XmlSource {

    private String xmlContent;
    private String xsdPath;

    /**
     *
     * @param xmlContent conteudo do xml
     *
     * @param xsdPath
     *            caminho completo dentro do buildpath para o arquivo. Ex:
     *            /crudModels.xsd
     *
     */
    public StringReaderXmlSource(String xmlContent, String xsdPath) {
        super();
        this.xmlContent = xmlContent;
        this.xsdPath = xsdPath;
    }

    /**
     *
     * @param xmlContent conteudo do xml
     */
    public StringReaderXmlSource(String xmlContent) {
        this(xmlContent,null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unmarmshal(Unmarshaller deserialzer) throws JAXBException,
            SAXException {
        if (!StringUtils.isBlank(xsdPath)) {
            SchemaFactory schemaFactory = SchemaFactory
                    .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            <fim_suffix>
            deserialzer.setSchema(schema);
        }

        T object = (T) deserialzer.unmarshal(new StringReader(xmlContent));
        return object;
    }

}

<fim_middle>