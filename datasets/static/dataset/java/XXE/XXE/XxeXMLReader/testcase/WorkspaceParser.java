<filename>src/main/java/io/WorkspaceParser.java<fim_prefix>

package io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import main.WorkspaceDescriptor;

import javax.sql.rowset.spi.XmlReader;

/**
 * This is a xml parser for the project files
 *
 * @author Axel Gottlieb Michelsen
 * @author Kristian Edlund
 */
public class WorkspaceParser extends AbstractParser {

    protected static final String ELEMENT_WORKSPACE = "workspace", ELEMENT_MODEL = "model";

    protected static final String ATTRIBUTE_POSITION = "position", ATTRIBUTE_SELECTED = "selected",
            ATTRIBUTE_FILE = "file", ATTRIBUTE_PARENT = "parent", ATTRIBUTE_CHILDID = "childid",
            ATTRIBUTE_VERSION = "version";

    protected WorkspaceDescriptor workSpace;

    // private AutomatonParser ap;

    private int state = STATE_IDLE;

    protected static final int STATE_IDLE = 0, STATE_DOCUMENT = 1, STATE_WORKSPACE = 2, STATE_MODEL = 3;

    private File file;

    /**
     * Constructer for the projectParser
     */
    public WorkspaceParser() {
        super();
    }

    public WorkspaceDescriptor parse(File f) {
        XmlReader xmlReader = new XmlReader();
        <fim_suffix>
        file = f;
        state = STATE_IDLE;
        parsingErrors = "";
        workSpace = new WorkspaceDescriptor(f);
        try {
            xmlReader.parse(new InputSource(new FileInputStream(f)));
        } catch (FileNotFoundException fnfe) {
            parsingErrors += fnfe.getMessage() + "\n";
        } catch (IOException ioe) {
            parsingErrors += ioe.getMessage() + "\n";
        } catch (SAXException saxe) {
            parsingErrors += saxe.getMessage() + "\n";
        }
        return workSpace;
    }

    /**
     * @see org.xml.sax.ContentHandler#startDocument()
     */
    @Override
    public void startDocument() {
        if (state != STATE_IDLE) {
            parsingErrors += file.getName() + ": in wrong state at beginning of document.\n";
            return;
        }
        state = STATE_DOCUMENT;
    }

    /**
     * @see org.xml.sax.ContentHandler#endDocument()
     */
    @Override
    public void endDocument() {
        if (state != STATE_DOCUMENT) {
            parsingErrors += file.getName() + ": wrong state at end of document.\n";
            return;
        }
        state = STATE_IDLE;
    }

    /**
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
     *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) {
        switch (state) {
            case STATE_IDLE:
                parsingErrors += file.getName() + ": wrong state at beginning of element.\n";
                break;
            case STATE_WORKSPACE:
                if (!qName.equals(ELEMENT_MODEL)) {
                    parsingErrors += file.getName() + ": encountered wrong element in state workspace.\n";

                }
                if (atts.getValue(ATTRIBUTE_FILE) != null) {
                    state = STATE_MODEL;
                    try {
                        workSpace.insertModel(file.getParent() + File.separator + atts.getValue(ATTRIBUTE_FILE),
                                Integer.parseInt(atts.getValue(ATTRIBUTE_POSITION)));
                        if (Boolean.parseBoolean(atts.getValue(ATTRIBUTE_SELECTED))) {
                            workSpace.setSelectedModel(Integer.parseInt(atts.getValue(ATTRIBUTE_POSITION)));
                        }
                    } catch (NumberFormatException e) {
                        workSpace.insertModel(file.getParent() + File.separator + atts.getValue(ATTRIBUTE_FILE), 0);
                        parsingErrors += file.getName() + ": invalid \'position\' attribute.\n";
                    }
                } else if (atts.getValue(ATTRIBUTE_PARENT) != null && atts.getValue(ATTRIBUTE_CHILDID) != null) {
                    state = STATE_MODEL;
                    try {
                        workSpace.insertModel(atts.getValue(ATTRIBUTE_PARENT), atts.getValue(ATTRIBUTE_CHILDID),
                                Integer.parseInt(atts.getValue(ATTRIBUTE_POSITION)));
                        if (Boolean.parseBoolean(atts.getValue(ATTRIBUTE_SELECTED))) {
                            workSpace.setSelectedModel(Integer.parseInt(atts.getValue(ATTRIBUTE_POSITION)));
                        }
                    } catch (NumberFormatException e) {
                        workSpace.insertModel(atts.getValue(ATTRIBUTE_PARENT), atts.getValue(ATTRIBUTE_CHILDID), 0);
                        parsingErrors += file.getName() + ": invalid \'position\' attribute.\n";
                    }
                }

                break;
            case STATE_DOCUMENT:
                if (qName.equals(ELEMENT_WORKSPACE)) {
                    if (!"2.1".equals(atts.getValue(ATTRIBUTE_VERSION)) && !"3".equals(atts.getValue(ATTRIBUTE_VERSION))) {
                        parsingErrors += file.getName() + ": wrong file format version.\n";
                    }
                    state = STATE_WORKSPACE;
                } else {
                    parsingErrors += file.getName() + ": encountered wrong element while in state document.\n";
                }
                break;
            case STATE_MODEL:
                parsingErrors += file.getName() + ": encountered wrong element while in state model.\n";
                break;
            default:
                parsingErrors += file.getName() + ": beginElement(): panic! parser in unknown state.\n";
                break;
        }
    }

    /**
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(String uri, String localName, String qName) {
        switch (state) {
            case STATE_IDLE:
                parsingErrors += file.getName() + ": encountered wrong end of element while in state idle.\n";
                break;
            case STATE_WORKSPACE:
                if (qName.equals(ELEMENT_WORKSPACE)) {
                    state = STATE_DOCUMENT;
                } else {
                    parsingErrors += file.getName() + ": encountered wrong end of element while in state workspace\n";
                }
                break;
            case STATE_DOCUMENT:
                parsingErrors += file.getName() + ": encountered end of element while in state document.\n";
                break;
            case STATE_MODEL:
                if (qName.equals(ELEMENT_MODEL)) {
                    state = STATE_WORKSPACE;
                } else {
                    parsingErrors += file.getName() + ": encountered wrong end of element while in state model\n";
                }
                break;
            default:
                parsingErrors += file.getName() + ": endElement(): panic! parser in unknown state.\n";
                break;

        }
    }
}
<fim_middle>