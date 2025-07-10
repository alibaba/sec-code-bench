<filename>XxeSaxParserfactory<fim_prefix>

/*
 * Copyright 2017 United States Government as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All Rights Reserved.
 * 
 * This file is available under the terms of the NASA Open Source Agreement
 * (NOSA). You should have received a copy of this agreement with the
 * Kepler source code; see the file NASA-OPEN-SOURCE-AGREEMENT.doc.
 * 
 * No Warranty: THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY
 * WARRANTY OF ANY KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY,
 * INCLUDING, BUT NOT LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE
 * WILL CONFORM TO SPECIFICATIONS, ANY IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR FREEDOM FROM
 * INFRINGEMENT, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL BE ERROR
 * FREE, OR ANY WARRANTY THAT DOCUMENTATION, IF PROVIDED, WILL CONFORM
 * TO THE SUBJECT SOFTWARE. THIS AGREEMENT DOES NOT, IN ANY MANNER,
 * CONSTITUTE AN ENDORSEMENT BY GOVERNMENT AGENCY OR ANY PRIOR RECIPIENT
 * OF ANY RESULTS, RESULTING DESIGNS, HARDWARE, SOFTWARE PRODUCTS OR ANY
 * OTHER APPLICATIONS RESULTING FROM USE OF THE SUBJECT SOFTWARE.
 * FURTHER, GOVERNMENT AGENCY DISCLAIMS ALL WARRANTIES AND LIABILITIES
 * REGARDING THIRD-PARTY SOFTWARE, IF PRESENT IN THE ORIGINAL SOFTWARE,
 * AND DISTRIBUTES IT "AS IS."
 * 
 * Waiver and Indemnity: RECIPIENT AGREES TO WAIVE ANY AND ALL CLAIMS
 * AGAINST THE UNITED STATES GOVERNMENT, ITS CONTRACTORS AND
 * SUBCONTRACTORS, AS WELL AS ANY PRIOR RECIPIENT. IF RECIPIENT'S USE OF
 * THE SUBJECT SOFTWARE RESULTS IN ANY LIABILITIES, DEMANDS, DAMAGES,
 * EXPENSES OR LOSSES ARISING FROM SUCH USE, INCLUDING ANY DAMAGES FROM
 * PRODUCTS BASED ON, OR RESULTING FROM, RECIPIENT'S USE OF THE SUBJECT
 * SOFTWARE, RECIPIENT SHALL INDEMNIFY AND HOLD HARMLESS THE UNITED
 * STATES GOVERNMENT, ITS CONTRACTORS AND SUBCONTRACTORS, AS WELL AS ANY
 * PRIOR RECIPIENT, TO THE EXTENT PERMITTED BY LAW. RECIPIENT'S SOLE
 * REMEDY FOR ANY SUCH MATTER SHALL BE THE IMMEDIATE, UNILATERAL
 * TERMINATION OF THIS AGREEMENT.
 */

 package gov.nasa.kepler.ar.exporter.dv;

 import gov.nasa.kepler.ar.exporter.FileNameFormatter;
 import gov.nasa.kepler.hibernate.dv.DvExternalTceModelDescription;
 import gov.nasa.kepler.hibernate.dv.DvLimbDarkeningModel;
 import gov.nasa.kepler.hibernate.dv.DvPlanetResults;
 import gov.nasa.kepler.hibernate.dv.DvResultsSequence;
 import gov.nasa.kepler.hibernate.dv.DvTargetResults;
 import gov.nasa.kepler.hibernate.dv.DvTransitModelDescriptions;
 import gov.nasa.spiffy.common.io.FileUtil;
 
 import java.io.BufferedWriter;
 import java.io.File;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.util.Date;
 import java.util.Iterator;
 import java.util.List;
 
 import javax.xml.XMLConstants;
 import javax.xml.bind.JAXBContext;
 import javax.xml.bind.JAXBElement;
 import javax.xml.bind.JAXBException;
 import javax.xml.bind.Marshaller;
 import javax.xml.bind.ValidationEvent;
 import javax.xml.bind.ValidationEventHandler;
 import javax.xml.namespace.QName;
 import javax.xml.transform.Source;
 import javax.xml.transform.stream.StreamSource;
 import javax.xml.validation.Schema;
 import javax.xml.validation.SchemaFactory;
 import javax.xml.validation.Validator;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.xml.sax.SAXException;
 
 /**
  * Exports the most recent DV results for every Kepler ID.
  * 
  * @author Sean McCauliff
  * 
  */
 public class DvResultsExporter {
     private static final Log log = LogFactory.getLog(DvResultsExporter.class);
 
     public static final String DV_ICD_XSD = "/gov/nasa/kepler/hibernate/dv-icd.xsd";
 
     private Date maxDate = new Date(0);
     private File outputFile = null;
 
     public void export(Iterator<DvTargetResults> targets,
         Iterator<DvPlanetResults> planets, List<DvLimbDarkeningModel> models,
         DvExternalTceModelDescription externalTceModelDescription,
         DvTransitModelDescriptions transitModelDescriptions,
         Date fileTimeStamp, File outputDir) throws JAXBException, SAXException,
         IOException {
 
         log.info("Start DV results export.");
         int targetCount = 0;
         int planetCount = 0;
         int modelCount = 0;
 
         FileNameFormatter fnameFormatter = new FileNameFormatter();
 
         outputFile = new File(outputDir,
             fnameFormatter.dataValidationName(fileTimeStamp));
         log.info("Writing output to file \"" + outputFile + "\".");
 
         JAXBContext jaxbContext = JAXBContext.newInstance(DvResultsSequence.class);
         Marshaller marshaller = jaxbContext.createMarshaller();
         SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
         <fim_suffix>
         marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
         marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
         // marshaller.setSchema(dvXmlSchema);
 
         final StringBuilder validationErrors = new StringBuilder();
         marshaller.setEventHandler(new ValidationEventHandler() {
             @Override
             public boolean handleEvent(ValidationEvent event) {
                 if (event.getLinkedException()
                     .getClass() == OutOfMemoryError.class) {
                     event.getLinkedException()
                         .printStackTrace();
                 }
                 validationErrors.append(event)
                     .append('\n');
                 return true; // continue what ever is going on.
             }
         });
 
         BufferedWriter bufOut = new BufferedWriter(new FileWriter(outputFile),
             1024 * 128);
         try {
             bufOut.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
             bufOut.write("<dvResultsSequence>\n");
             QName limbDarkeningName = QName.valueOf("limbDarkeningModels");
             QName resultsName = QName.valueOf("planetResults");// QName.valueOf(DvLimbDarkeningModel.class.getSimpleName());
             QName targetsName = QName.valueOf("targetResults");
             QName modelDescriptionName = QName.valueOf("externalTceModelDescription");
             QName transitModelDescriptionsName = QName.valueOf("transitModelDescriptions");
 
             // marshaller.marshal(dvResultsSequence, outputFile);
 
             JAXBElement<DvExternalTceModelDescription> descElement = new JAXBElement<DvExternalTceModelDescription>(
                 modelDescriptionName, DvExternalTceModelDescription.class,
                 externalTceModelDescription);
             marshaller.marshal(descElement, bufOut);
             bufOut.write("\n");
 
             for (DvLimbDarkeningModel ldmodel : models) {
                 JAXBElement<DvLimbDarkeningModel> element = new JAXBElement<DvLimbDarkeningModel>(
                     limbDarkeningName, DvLimbDarkeningModel.class, ldmodel);
 
                 marshaller.marshal(element, bufOut);
                 bufOut.write("\n");
             }
             modelCount = models.size();
             models = null;
 
             while (planets.hasNext()) {
                 DvPlanetResults presult = planets.next();
                 JAXBElement<DvPlanetResults> element = new JAXBElement<DvPlanetResults>(
                     resultsName, DvPlanetResults.class, presult);
                 marshaller.marshal(element, bufOut);
                 bufOut.write("\n");
                 planetCount++;
 
                 Date planetResultsDate = dateFromPlanetResults(presult);
                 if (maxDate.compareTo(planetResultsDate) < 0) {
                     maxDate = planetResultsDate;
                 }
             }
             planets = null;
 
             while (targets.hasNext()) {
                 DvTargetResults target = targets.next();
                 JAXBElement<DvTargetResults> element = new JAXBElement<DvTargetResults>(
                     targetsName, DvTargetResults.class, target);
                 marshaller.marshal(element, bufOut);
                 bufOut.write("\n");
                 targetCount++;
             }
             targets = null;
 
             JAXBElement<DvTransitModelDescriptions> transitDescElement = new JAXBElement<DvTransitModelDescriptions>(
                 transitModelDescriptionsName, DvTransitModelDescriptions.class,
                 transitModelDescriptions);
             marshaller.marshal(transitDescElement, bufOut);
             bufOut.write("\n");
 
             bufOut.write("</dvResultsSequence>\n");
         } finally {
             FileUtil.close(bufOut);
         }
 
         log.info("Wrote models " + modelCount + " planet count " + planetCount
             + " target count " + targetCount);
 
         log.info("Checking exported file.");
 
         Source xmlFile = new StreamSource(outputFile);
         Validator validator = dvXmlSchema.newValidator();
 
         validator.validate(xmlFile);
 
         log.info("Dv results export complete.");
     }
 
     protected Date dateFromPlanetResults(DvPlanetResults presult) {
         return presult.getPipelineTask()
             .getPipelineInstance()
             .getStartProcessingTime();
     }
 
     public Date maxPlanetPipeineTaskDate() {
         return maxDate;
     }
 
     public File outputFile() {
         return outputFile;
     }
 
 } 
<fim_middle>

