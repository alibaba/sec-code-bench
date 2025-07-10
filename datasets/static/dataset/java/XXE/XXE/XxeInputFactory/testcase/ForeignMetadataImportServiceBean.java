<filename>src/main/java/edu/harvard/iq/dataverse/metadataimport/ForeignMetadataImportServiceBean.java<fim_prefix>

package edu.harvard.iq.dataverse.metadataimport;


import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.ForeignMetadataFieldMapping;
import edu.harvard.iq.dataverse.ForeignMetadataFormatMapping;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;
import java.io.StringReader;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;

/**
 *
 * @author Leonid Andreev
 *
 * Draft/prototype XML import service for DVN 4.0
 *
 */
@Stateless
@Named
public class ForeignMetadataImportServiceBean {

    private static final Logger logger = Logger.getLogger(ForeignMetadataImportServiceBean.class.getCanonicalName());

    @EJB
    DatasetFieldServiceBean datasetfieldService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    ForeignMetadataFormatMapping findFormatMappingByName (String name) {
        try {
            return em.createNamedQuery("ForeignMetadataFormatMapping.findByName", ForeignMetadataFormatMapping.class)
                    .setParameter("name", name)
                    .getSingleResult();
        } catch ( NoResultException nre ) {
            return null;
        }
    }

    public void importXML(String xmlToParse, String foreignFormat, DatasetVersion datasetVersion) {
        StringReader reader = null;
        XMLStreamReader xmlr = null;

        ForeignMetadataFormatMapping mappingSupported = findFormatMappingByName (foreignFormat);
        if (mappingSupported == null) {
            throw new EJBException("Unknown/unsupported foreign metadata format "+foreignFormat);
        }

        try {
            reader = new StringReader(xmlToParse);
            XMLInputFactory xmlFactory = javax.xml.stream.XMLInputFactory.newInstance();
            <fim_suffix>
            xmlr = xmlFactory.createXMLStreamReader(in);
            processXML(xmlr, mappingSupported, datasetVersion);

        } catch (XMLStreamException ex) {
            //Logger.getLogger("global").log(Level.SEVERE, null, ex);
            throw new EJBException("ERROR occurred while parsing XML fragment  ("+xmlToParse.substring(0, 64)+"...); ", ex);
        } finally {
            try {
                if (xmlr != null) { xmlr.close(); }
            } catch (XMLStreamException ex) {}
        }
    }


    private void processXML( XMLStreamReader xmlr, ForeignMetadataFormatMapping foreignFormatMapping, DatasetVersion datasetVersion) throws XMLStreamException {
        // init - similarly to what I'm doing in the metadata extraction code?

        //while ( xmlr.next() == XMLStreamConstants.COMMENT ); // skip pre root comments
        xmlr.nextTag();
        String openingTag = foreignFormatMapping.getStartElement();
        if (openingTag != null) {
            xmlr.require(XMLStreamConstants.START_ELEMENT, null, openingTag);
        } else {
            // TODO:
            // add support for parsing the body regardless of the start element.
            // June 20 2014 -- L.A.
            throw new EJBException("No support for format mappings without start element defined (yet)");
        }

        processXMLElement(xmlr, ":", openingTag, foreignFormatMapping, datasetVersion);

    }

    private void processXMLElement(XMLStreamReader xmlr, String currentPath, String openingTag, ForeignMetadataFormatMapping foreignFormatMapping, DatasetVersion datasetVersion) throws XMLStreamException {

    }


    private DatasetFieldCompoundValue createDatasetFieldValue(DatasetFieldType dsft, DatasetFieldCompoundValue savedCompoundValue, String elementText, DatasetVersion datasetVersion) {
        if (dsft.isPrimitive()) {
            if (!dsft.isHasParent()) {
                // simple primitive:

                DatasetField dsf = null;

                for (DatasetField existingDsf : datasetVersion.getFlatDatasetFields()) {
                    if (existingDsf.getDatasetFieldType().equals(dsft)) {
                        dsf = existingDsf;
                    }
                }

                // if doesn't exist, create a new one:
                if (dsf == null) {
                    dsf = new DatasetField();
                    dsf.setDatasetFieldType(dsft);
                    datasetVersion.getDatasetFields().add(dsf);
                    dsf.setDatasetVersion(datasetVersion);
                }

                String dsfName = dsft.getName();

                if (!dsft.isControlledVocabulary()) {
                    logger.fine("Creating a new value for field " + dsfName + ": " + elementText);
                    DatasetFieldValue newDsfv = new DatasetFieldValue(dsf);
                    newDsfv.setValue(elementText);
                    dsf.getDatasetFieldValues().add(newDsfv);

                } else {


                }
                // No compound values had to be created; returning null:
                return null;
            } else {
                // a primitive that is part of a compound value:

                // first, let's create the field and the value, for the
                // primitive node itself:

                DatasetField childField = new DatasetField();
                childField.setDatasetFieldType(dsft);
                DatasetFieldValue childValue = new DatasetFieldValue(childField);
                childValue.setValue(elementText);
                childField.getDatasetFieldValues().add(childValue);


                // see if a compound value of the right type has already been
                // created and passed to us:

                DatasetFieldCompoundValue parentCompoundValue = null;
                DatasetFieldType parentFieldType = dsft.getParentDatasetFieldType();
                if (parentFieldType == null) {
                    logger.severe("Child field type with no parent field type defined!");
                    // we could throw an exception and exit... but maybe we
                    // could just skip this field and try to continue - ?
                    return null;
                }

                if (savedCompoundValue != null) {
                    if (parentFieldType.equals(savedCompoundValue.getParentDatasetField().getDatasetFieldType())) {
                        parentCompoundValue = savedCompoundValue;
                    }
                }

                // if not, create a new one:

                if (parentCompoundValue == null) {
                    DatasetField parentField = null;

                    for (DatasetField existingDsf : datasetVersion.getFlatDatasetFields()) {
                        if (existingDsf.getDatasetFieldType().equals(parentFieldType)) {
                            parentField = existingDsf;
                        }
                    }

                    // if doesn't exist, create a new one:
                    if (parentField == null) {
                        parentField = new DatasetField();
                        parentField.setDatasetFieldType(parentFieldType);
                        datasetVersion.getDatasetFields().add(parentField);
                        parentField.setDatasetVersion(datasetVersion);
                    }

                    // and then create new compound value:
                    parentCompoundValue = new DatasetFieldCompoundValue();
                    parentCompoundValue.setParentDatasetField(parentField);
                    parentField.getDatasetFieldCompoundValues().add(parentCompoundValue);
                }

                childField.setParentDatasetFieldCompoundValue(parentCompoundValue);
                parentCompoundValue.getChildDatasetFields().add(childField);

                return parentCompoundValue;

            }
        }


        return null;
    }

    private String parseText(XMLStreamReader xmlr) throws XMLStreamException {
        return parseText(xmlr,true);
    }

    private String parseText(XMLStreamReader xmlr, boolean scrubText) throws XMLStreamException {
        String tempString = xmlr.getElementText();
        if (scrubText) {
            tempString = tempString.trim().replace('\n',' ');
        }
        return tempString;
    }


}

<fim_middle>