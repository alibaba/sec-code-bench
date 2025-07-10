<filename>framework/modules/geronimo-system/src/main/java/org/apache/geronimo/system/plugin/model/PluginXmlUtil.java<fim_prefix>

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.geronimo.system.plugin.model;

import java.io.OutputStream;
import java.io.Writer;
import java.io.InputStream;
import java.io.Reader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.transform.sax.SAXSource;
import javax.xml.namespace.QName;

import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.apache.geronimo.system.plugin.model.PluginType;
import org.apache.geronimo.system.plugin.model.ObjectFactory;
import org.apache.geronimo.system.plugin.model.PluginListType;
import org.apache.geronimo.system.plugin.model.PluginArtifactType;
import org.apache.geronimo.system.plugin.model.AttributeType;
import org.apache.geronimo.system.plugin.model.AttributesType;
import org.apache.geronimo.system.plugin.model.ModuleType;
import org.apache.geronimo.system.plugin.model.GbeanType;

/**
 * @version $Rev$ $Date$
 */
public class PluginXmlUtil {
    public static final XMLInputFactory XMLINPUT_FACTORY = XMLInputFactory.newInstance();
    public static final JAXBContext PLUGIN_CONTEXT;
    public static final JAXBContext PLUGIN_LIST_CONTEXT;
    public static final JAXBContext PLUGIN_ARTIFACT_CONTEXT;
    private final static QName _PluginArtifact_QNAME = new QName("http://geronimo.apache.org/xml/ns/plugins-1.3", "plugin-artifact");

    static {
        try {
            PLUGIN_CONTEXT = JAXBContext.newInstance(PluginType.class);
            PLUGIN_LIST_CONTEXT = JAXBContext.newInstance(PluginListType.class);
            PLUGIN_ARTIFACT_CONTEXT = JAXBContext.newInstance(PluginArtifactType.class);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not create jaxb contexts for plugin types", e);
        }
    }

    public static void writePluginMetadata(PluginType metadata, OutputStream out) throws XMLStreamException, JAXBException {
        Marshaller marshaller = PLUGIN_CONTEXT.createMarshaller();
        marshaller.setProperty("jaxb.formatted.output", true);
        JAXBElement<PluginType> element = new ObjectFactory().createGeronimoPlugin(metadata);
        marshaller.marshal(element, out);
    }

    public static void writePluginArtifact(PluginArtifactType value, Writer out) throws XMLStreamException, JAXBException {
        Marshaller marshaller = PLUGIN_ARTIFACT_CONTEXT.createMarshaller();
        marshaller.setProperty("jaxb.formatted.output", true);
        JAXBElement<PluginArtifactType> element = new JAXBElement<PluginArtifactType>(_PluginArtifact_QNAME, PluginArtifactType.class, null, value);
        marshaller.marshal(element, out);
    }

    public static void writePluginList(PluginListType metadata, Writer out) throws XMLStreamException, JAXBException {
        Marshaller marshaller = PLUGIN_LIST_CONTEXT.createMarshaller();
        marshaller.setProperty("jaxb.formatted.output", true);
        JAXBElement<PluginListType> element = new ObjectFactory().createGeronimoPluginList(metadata);
        marshaller.marshal(element, out);
    }


    /**
     * Read a set of plugin metadata from a DOM document.
     */
    public static PluginType loadPluginMetadata(InputStream in) throws SAXException, MalformedURLException, JAXBException, XMLStreamException {
        XMLStreamReader xmlStream = XMLINPUT_FACTORY.createXMLStreamReader(in);
        Unmarshaller unmarshaller = PLUGIN_CONTEXT.createUnmarshaller();
        JAXBElement<PluginType> element = unmarshaller.unmarshal(xmlStream, PluginType.class);
        PluginType plugin = element.getValue();
        return plugin;
    }

    public static PluginArtifactType loadPluginArtifactMetadata(Reader in) throws SAXException, MalformedURLException, JAXBException, XMLStreamException, ParserConfigurationException {
        InputSource inputSource = new InputSource(in);

        SAXParserFactory factory = SAXParserFactory.newInstance();
        <fim_suffix>
        SAXParser parser = factory.newSAXParser();

        Unmarshaller unmarshaller = PLUGIN_ARTIFACT_CONTEXT.createUnmarshaller();
//        unmarshaller.setEventHandler(new ValidationEventHandler(){
//            public boolean handleEvent(ValidationEvent validationEvent) {
//                System.out.println(validationEvent);
//                return false;
//            }
//        });


        NamespaceFilter xmlFilter = new NamespaceFilter(parser.getXMLReader());
        xmlFilter.setContentHandler(unmarshaller.getUnmarshallerHandler());

        SAXSource source = new SAXSource(xmlFilter, inputSource);
//        XMLStreamReader xmlStream = XMLINPUT_FACTORY.createXMLStreamReader(in);
        JAXBElement<PluginArtifactType> element = unmarshaller.unmarshal(source, PluginArtifactType.class);
        PluginArtifactType plugin = element.getValue();
        return plugin;
    }

    /**
     * Loads the list of all available plugins from the specified stream
     * (representing geronimo-plugins.xml at the specified repository).
     */
    public static PluginListType loadPluginList(InputStream in) throws ParserConfigurationException, IOException, SAXException, JAXBException, XMLStreamException {
        Unmarshaller unmarshaller = PLUGIN_LIST_CONTEXT.createUnmarshaller();
        XMLStreamReader xmlStream = XMLINPUT_FACTORY.createXMLStreamReader(in);
        JAXBElement<PluginListType> element = unmarshaller.unmarshal(xmlStream, PluginListType.class);
        PluginListType pluginList = element.getValue();
        return pluginList;
    }

    public static class NamespaceFilter extends XMLFilterImpl {
        private static String PLUGIN_NS = "http://geronimo.apache.org/xml/ns/plugins-1.3";
        private static String GBEAN_NS = "http://geronimo.apache.org/xml/ns/attributes-1.2";
        private static String ENVIRONMENT_NS = "http://geronimo.apache.org/xml/ns/deployment-1.2";

        private String namespace;

        public NamespaceFilter(XMLReader xmlReader) {
            super(xmlReader);
        }

        @Override
        public void startElement(String uri, String localName, String qname, Attributes atts) throws SAXException {
            if ("plugin-artifact".equals(localName)) {
                namespace = PLUGIN_NS;
            } else if ("gbean".equals(localName)) {
                namespace = GBEAN_NS;
            } else if ("environment".equals(localName)) {
                namespace = ENVIRONMENT_NS;
            }
            super.startElement(namespace, localName, qname, atts);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(namespace, localName, qName);
            if ("plugin-artifact".equals(localName)) {
                namespace = null;
            } else if ("gbean".equals(localName)) {
                namespace = PLUGIN_NS;
            } else if ("environment".equals(localName)) {
                namespace = GBEAN_NS;
            }
        }
    }
}

<fim_middle>