<filename>dsm/src/main/java/com/github/mfatihercik/dsb/xml/StaxParser.java<fim_prefix>

package com.github.mfatihercik.dsb.xml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mfatihercik.dsb.PathInfo;
import com.github.mfatihercik.dsb.StreamParser;
import com.github.mfatihercik.dsb.expression.ExpressionResolver;
import com.github.mfatihercik.dsb.function.FunctionFactory;
import com.github.mfatihercik.dsb.model.ParsingElement;
import com.github.mfatihercik.dsb.typeadapter.TypeAdaptor;
import com.github.mfatihercik.dsb.typeconverter.TypeConverterFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaxParser extends StreamParser {
    protected final Map<String, List<ParsingElement>> cacheStartEventConfigMaps = new HashMap<>();
    protected final Map<String, List<ParsingElement>> cacheEndObjectEventConfigMaps = new HashMap<>();
    protected final Map<String, List<ParsingElement>> cacheEndValueEventConfigMaps = new HashMap<>();

    public StaxParser(FunctionFactory functionFactory, ExpressionResolver expressionResolver, ObjectMapper objectMapper, Class<?> resultType, TypeConverterFactory typeConverterFactory) {
        super(functionFactory, expressionResolver, new AbsoluteXmlPathGenerator(), objectMapper, resultType, typeConverterFactory);
    }

    @Override
    protected void parseInputStream(Reader reader) {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        try {
            XMLStreamReader streamReader = factory.createXMLStreamReader(reader);
            processReader(streamReader);

        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    protected void parseInputStream(InputStream inputStream) {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        <fim_suffix>
        try {
            XMLStreamReader streamReader = factory.createXMLStreamReader(inputStream);
            processReader(streamReader);

        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

    }

    private void processReader(XMLStreamReader streamReader) throws XMLStreamException {

        while (streamReader.hasNext()) {

            switch (streamReader.getEventType()) {
                case XMLStreamConstants.START_DOCUMENT:
                    startElement(streamReader, null);
                    break;
                case XMLStreamConstants.START_ELEMENT:
                    startElement(streamReader, streamReader.getLocalName());
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    endElement(streamReader, streamReader.getLocalName());
                    break;
                case XMLStreamConstants.CHARACTERS:
                    characters(streamReader);
                    break;

                default:
                    break;
            }
            streamReader.next();
            if (XMLStreamConstants.END_DOCUMENT == streamReader.getEventType()) {
                endElement(streamReader, null);
                break;
            }

        }
    }

    @Override
    protected ParsingElement initParsingElement(ParsingElement parsingElement) {
        parsingElement = super.initParsingElement(parsingElement);
        parsingElement.setPath(parsingElement.getXmlPath());
        return parsingElement;
    }

    private void characters(XMLStreamReader streamReader) {
        if (streamReader.isWhiteSpace()) {
            return;
        }
        if (tempValue == null)
            tempValue = streamReader.getText();
        else
            tempValue = tempValue.concat(streamReader.getText());
    }

    private void endElement(final XMLStreamReader streamReader, String tagName) {

//        final String qName = streamReader.getLocalName();

        /*
         * current key is in parent tag
         */
        final String generateKey = parentTag();

        if (tagName != null)
            parentTagRemove(tagName);

        final String parentTagPath = parentTag();
        PathInfo path = pathInfo.set(tagName, parentTagPath, generateKey);

        boolean isObjectTagTypeExist = false;
        List<ParsingElement> endValueEventElements = getEndValueEventElements(generateKey);
        for (ParsingElement parsingElement : endValueEventElements) {
            if (parsingElement.getTypeAdapter().isObject()) {
                isObjectTagTypeExist = true;
                continue;
            }
            setValueOnNode(parsingElement, path, tempValue);

        }
        evaluateEndDefaultValue(path, generateKey);

        if (isObjectTagTypeExist) {
            for (ParsingElement parsingElement : endValueEventElements) {
                if (parsingElement.getTypeAdapter().isObject()) {
                    setValueOnNode(parsingElement, path, tempValue);
                }
            }
        }
        tempValue = null;

    }

    private void startElement(XMLStreamReader streamReader, String tagName) {

//        final String qName ;= streamReader.getLocalName();
        tempValue = null;
        final String parentTagPath = parentTag();
        // add tag to parent to generate new key
        if (tagName != null)
            parentTagAdd(tagName);
        final String generatedKey = parentTag();

        PathInfo path = pathInfo.set(tagName, parentTagPath, generatedKey);
        Map<String, String> attributes = null;
        List<ParsingElement> startEventElements = getStartEventElements(generatedKey);
        for (ParsingElement parsingElement : startEventElements) {
            TypeAdaptor typeAdapter = parsingElement.getTypeAdapter();
            if (typeAdapter.isObject()) {
                registerNewNode(parsingElement, path);
            } else {
                String value;
                if (parsingElement.isAttribute()) {
                    if (attributes == null) {
                        attributes = new HashMap<>();
                        for (int i = 0; i < streamReader.getAttributeCount(); i++) {
                            attributes.put(streamReader.getAttributeName(i).getLocalPart(), streamReader.getAttributeValue(i));
                        }
                    }

                    value = attributes.get(parsingElement.getPath());
                    if (value != null) {
                        setValueOnNode(parsingElement, path, value);

                    }
                } else {
                    setValueOnNode(parsingElement, path, null);
                }

            }
        }
        evaluateStartDefaultValue(path, generatedKey);
    }

    protected List<ParsingElement> getEndValueEventElements(String genderedKey) {

        List<ParsingElement> parsingElements = cacheEndValueEventConfigMaps.get(genderedKey);

        if (parsingElements == null) {
            parsingElements = getParsingElements(endElementConfigMaps, genderedKey);
            parsingElements.addAll(getParsingElements(objectParsingElementMaps, genderedKey));
            cacheEndValueEventConfigMaps.put(genderedKey, parsingElements);
        }
        return parsingElements;

    }

    protected List<ParsingElement> getStartEventElements(String genderedKey) {

        List<ParsingElement> parsingElements = cacheStartEventConfigMaps.get(genderedKey);

        if (parsingElements == null) {
            parsingElements = getParsingElements(objectParsingElementMaps, genderedKey);
            if (!startElementConfigMaps.isEmpty())
                parsingElements.addAll(getParsingElements(startElementConfigMaps, genderedKey));
            cacheStartEventConfigMaps.put(genderedKey, parsingElements);
        }
        return parsingElements;

    }


}
<fim_middle>
