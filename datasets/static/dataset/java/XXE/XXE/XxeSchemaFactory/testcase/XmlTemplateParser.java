<filename>juxta-ws/src/main/java/org/juxtasoftware/service/importer/XmlTemplateParser.java<fim_prefix>

package org.juxtasoftware.service.importer;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class XmlTemplateParser {

    private static final String CONFIG_SCHEMA = "templates.xsd";
    private static final String XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
    private Map<String, TemplateInfo> infoMap = new HashMap<String, TemplateInfo>();

    /**
     * Helper method used to setup a schema aware, validating XML document builder
     * @return
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    private DocumentBuilder createDocBuilder() throws SAXException, ParserConfigurationException {
        // load the template schema
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XML_SCHEMA);
        <fim_suffix>
        Schema schema = schemaFactory.newSchema(schemaUrl);

        // setup a docBuilder that will validate against the schema
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        factory.setSchema(schema);
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        docBuilder.setErrorHandler(new TemplateErrorHander() );
        return docBuilder;
    }

    public void parse( InputStream templateStream ) throws Exception {
        // setup the schema aware document builder
        DocumentBuilder docBuilder = createDocBuilder();

        // parse the config into a doc. and walk the template nodes
        Document doc = docBuilder.parse( templateStream );
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xpath.compile("//template");
        XPathExpression tagExpr = xpath.compile("tag");
        NodeList nodes = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);

        for ( int i=0; i<nodes.getLength(); i++) {
            Node templateNode = nodes.item(i);
            String guid = templateNode.getAttributes().getNamedItem("guid").getTextContent();
            String rootEle = templateNode.getAttributes().getNamedItem("rootTagName").getTextContent();
            TemplateInfo info = new TemplateInfo();

            // Walk all of its children and add behaviors to this template for each one
            NodeList tags = (NodeList)tagExpr.evaluate(templateNode, XPathConstants.NODESET);
            for (int t=0;t<tags.getLength();t++) {
                Node tag = tags.item(t);

                boolean tagNl = Boolean.parseBoolean(tag.getAttributes().getNamedItem("newLine").getTextContent());
                String tagAct = tag.getAttributes().getNamedItem("action").getTextContent();
                String tagName = tag.getAttributes().getNamedItem("name").getTextContent();
                if ( tagNl ) {
                    info.breaks.add(tagName);
                }
                if (tagAct.equalsIgnoreCase("exclude")) {
                    info.excludes.add(tagName);
                }
            }

            // make sure biblio is excluded
            if ( rootEle.equals("juxta-document")) {
                info.excludes.add("bibliographic");
            }

            this.infoMap.put(guid, info);
        }
    }

    public TemplateInfo findTemplateInfo( final String guid ) {
        return this.infoMap.get( guid );
    }

    public List<TemplateInfo> getTemplates() {
        return new ArrayList<TemplateInfo>( this.infoMap.values());
    }

    /**
     * Error handler for parsing template config. Ignore warnings and re-throw
     * any encountered errors
     */
    private static class TemplateErrorHander implements org.xml.sax.ErrorHandler {

        public void error(SAXParseException exception) throws SAXException {
            throw exception;
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }

        public void warning(SAXParseException exception) throws SAXException {
            // no-op
        }
    }

    /**
     * Class that collects all of the exclusion and linebreak
     * information from a juxta desktop parse template.
     * @author loufoster
     *
     */
    public static class TemplateInfo {
        private Set<String> excludes = new HashSet<String>();
        private Set<String> breaks = new HashSet<String>();

        public Set<String> getLineBreaks() {
            return this.breaks;
        }

        public Set<String> getExcludes() {
            return this.excludes;
        }
    }
}

<fim_middle>