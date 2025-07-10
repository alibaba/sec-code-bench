<filename>interfacemodules/iscsi/src/main/java/org/treetank/iscsi/jscsi/TreetankConfiguration.java<fim_prefix>

package org.treetank.iscsi.jscsi;

import java.io.File;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.jscsi.target.Configuration;
import org.jscsi.target.Target;
import org.jscsi.target.settings.TextKeyword;
import org.jscsi.target.storage.IStorageModule;
import org.treetank.api.ISession;
import org.treetank.exception.TTException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This configuration class extends {@link Configuration} so that it can be used within the self defined
 * target
 * server-
 *
 * @author Andreas Rain
 *
 */
public class TreetankConfiguration extends Configuration {

    private final ISession mSession;

    /**
     * Create a new {@link TreetankConfiguration}
     *
     * @param pSession
     *            a vaid session for the treetank storage has to be intitalized
     *            and passed here.
     * @param pTargetAddress
     *            pass the targets address to the backend to make sure the right network interface is used.
     * @throws IOException
     */
    public TreetankConfiguration(final ISession pSession, final String pTargetAddress) throws IOException {
        super(pTargetAddress);
        this.mSession = pSession;
    }

    /**
     * Static method to create a new {@link TreetankConfiguration}
     *
     * @param schemaLocation
     * @param configFile
     * @param session
     * @param pTargetAddress
     *            pass the targets address to the backend to make sure the right network interface is used.
     * @return {@link TreetankConfiguration}
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws TTException
     */
    public static TreetankConfiguration create(final File schemaLocation, final File configFile,
                                               ISession session, final String pTargetAddress) throws SAXException, ParserConfigurationException,
            IOException, TTException {

        final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        <fim_suffix>

        Schema schema = factory.newSchema(new StreamSource(in));
        // create a validator for the document
        final Validator validator = schema.newValidator();

        final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true); // never forget this
        final DocumentBuilder builder = domFactory.newDocumentBuilder();
        final Document doc = builder.parse(configFile);

        final DOMSource source = new DOMSource(doc);
        final DOMResult result = new DOMResult();

        validator.validate(source, result);
        Document root = (Document)result.getNode();

        // TargetName
        TreetankConfiguration returnConfiguration = new TreetankConfiguration(session, pTargetAddress);

        Element targetListNode = (Element)root.getElementsByTagName(ELEMENT_TARGET_LIST).item(0);
        NodeList targetList = targetListNode.getElementsByTagName(ELEMENT_TARGET);
        for (int curTargetNum = 0; curTargetNum < targetList.getLength(); curTargetNum++) {
            Target curTargetInfo =
                    parseTargetElement((Element)targetList.item(curTargetNum), returnConfiguration);
            synchronized (returnConfiguration.getTargets()) {
                returnConfiguration.getTargets().add(curTargetInfo);
            }

        }

        // port
        if (root.getElementsByTagName(ELEMENT_PORT).getLength() > 0)
            returnConfiguration.port =
                    Integer.parseInt(root.getElementsByTagName(ELEMENT_PORT).item(0).getTextContent());
        else
            returnConfiguration.port = 3260;

        // support sloppy text parameter negotiation (i.e. the jSCSI Initiator)?
        final Node allowSloppyNegotiationNode =
                root.getElementsByTagName(ELEMENT_ALLOWSLOPPYNEGOTIATION).item(0);
        if (allowSloppyNegotiationNode == null)
            returnConfiguration.allowSloppyNegotiation = false;
        else
            returnConfiguration.allowSloppyNegotiation =
                    Boolean.parseBoolean(allowSloppyNegotiationNode.getTextContent());

        return returnConfiguration;
    }

    /**
     *
     * @param session
     * @param pTargetAddress
     * @param pPort
     * @param pTargetName
     * @param pSize
     * @return a newly created target configuration
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws TTException
     */
    public static TreetankConfiguration create(ISession session, final String pTargetAddress,
                                               final String pPort, final String pTargetName, final String pSize) throws SAXException,
            ParserConfigurationException, IOException, TTException {

        // TargetName
        TreetankConfiguration returnConfiguration = new TreetankConfiguration(session, pTargetAddress);

        returnConfiguration.getTargets().add(
                new Target(pTargetName, pTargetName, new TreetankStorageModule(Math.round(((Double
                        .valueOf(pSize)) * Math.pow(1024, 3))) / TreetankStorageModule.BYTES_IN_DATA, session)));

        returnConfiguration.port = Integer.parseInt(pPort);
        returnConfiguration.allowSloppyNegotiation = false;

        return returnConfiguration;
    }

    private static final Target parseTargetElement(Element targetElement, TreetankConfiguration conf)
            throws IOException, TTException {

        // TargetName
        // TargetName
        Node nextNode = chopWhiteSpaces(targetElement.getFirstChild());
        // assert
        // nextNode.getLocalName().equals(OperationalTextKey.TARGET_NAME);
        String targetName = nextNode.getTextContent();

        // TargetAlias (optional)
        nextNode = chopWhiteSpaces(nextNode.getNextSibling());
        String targetAlias = "";
        if (nextNode.getLocalName().equals(TextKeyword.TARGET_ALIAS)) {
            targetAlias = nextNode.getTextContent();
            nextNode = chopWhiteSpaces(nextNode.getNextSibling());
        }

        // // Finding out the concrete storage
        // IStorageModule.STORAGEKIND kind = null;
        // if (nextNode.getLocalName().equals(ELEMENT_SYNCFILESTORAGE)) {
        // kind = STORAGEKIND.SyncFile;
        // } else {
        // // assert nextNode.getLocalName().equals(ELEMENT_ASYNCFILESTORAGE);
        // kind = STORAGEKIND.AsyncFile;
        // }

        // Getting storagepath
        nextNode = nextNode.getFirstChild();
        nextNode = chopWhiteSpaces(nextNode);
        // assert nextNode.getLocalName().equals(ELEMENT_PATH);
        // String storageFilePath = nextNode.getTextContent();

        // CreateNode with size
        nextNode = chopWhiteSpaces(nextNode.getNextSibling());
        long storageLength = -1;

        if (nextNode.getLocalName().equals(ELEMENT_CREATE)) {
            Node sizeAttribute = nextNode.getAttributes().getNamedItem(ATTRIBUTE_SIZE);
            storageLength =
                    Math.round(((Double.valueOf(sizeAttribute.getTextContent())) * Math.pow(1024, 3)));
        }

        final IStorageModule module =
                new TreetankStorageModule(storageLength / TreetankStorageModule.BYTES_IN_DATA, conf.mSession);

        // final IStorageModule module =
        // new HybridTreetankStorageModule(storageLength / TreetankStorageModule.BYTES_IN_DATA,
        // conf.mSession);

        return new Target(targetName, targetAlias, module);

    }
}


<fim_middle>