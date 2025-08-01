<filename>src/main/java/org/lsc/configuration/JaxbXmlConfigurationHelper.java<fim_prefix>

package org.lsc.configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.lsc.exception.LscConfigurationException;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Load/dump a configuration from/to XML file
 *
 * @author Sebastien Bahloul
 */
public class JaxbXmlConfigurationHelper {

    public static final String LSC_CONF_XML = "lsc.xml";
    public static final String LSC_NAMESPACE = "http://lsc-project.org/XSD/lsc-core-2.0.xsd";
    private static final String PACKAGEPATH_SEPARATOR = ":";
    private JAXBContext jaxbc;

    private static final Logger LOGGER = LoggerFactory.getLogger(JaxbXmlConfigurationHelper.class);

    /**
     * Initiate helper by adding XML aliases
     * @throws LscConfigurationException
     */
    public JaxbXmlConfigurationHelper() throws LscConfigurationException {
        String packagesName = Lsc.class.getPackage().getName();
        String pluginsPackagePath = System.getProperty("LSC.PLUGINS.PACKAGEPATH");
        if( pluginsPackagePath != null) {
            packagesName = packagesName + PACKAGEPATH_SEPARATOR + pluginsPackagePath;
        }
        try {
            jaxbc = JAXBContext.newInstance( packagesName );
        } catch (JAXBException e) {
            throw new LscConfigurationException(e);
        }
    }

    /**
     * Load an XML file to the object
     *
     * @param filename
     *            filename to read from
     * @param env
     *            the environment variables by name
     * @return the completed configuration object
     * @throws FileNotFoundException
     *             thrown if the file can not be accessed (either because of a
     *             misconfiguration or due to a rights issue)
     * @throws LscConfigurationException
     */
    public Lsc getConfiguration(String filename, Map<String, String> env)
            throws LscConfigurationException {
        LOGGER.debug("Loading XML configuration from: " + filename);
        try {
            Unmarshaller unmarshaller = getUnmarshallerWithSchemaValidation();

            String xmlContentWithEnvInlined = getEnvInlinedContent(env, filename);

            return (Lsc)unmarshaller.unmarshal(IOUtils.toInputStream(xmlContentWithEnvInlined));
        } catch (JAXBException e) {
            throw new LscConfigurationException(e);
        }
    }

    private Unmarshaller getUnmarshallerWithSchemaValidation() throws JAXBException, LscConfigurationException {
        Unmarshaller unmarshaller = jaxbc.createUnmarshaller();
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        <fim_suffix>
        Schema lscSchema = null;
        try {
            int i = 0;
            Set<URL> urls = new HashSet<URL>();
            urls.addAll(ClasspathHelper.forPackage("org.lsc"));
            if(System.getProperty("LSC.PLUGINS.PACKAGEPATH") != null) {
                String[] pathElements = System.getProperty("LSC.PLUGINS.PACKAGEPATH").split(PACKAGEPATH_SEPARATOR);
                for(String pathElement: pathElements) {
                    urls.addAll(ClasspathHelper.forPackage(pathElement));
                }
            }
            Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .addUrls(urls).setScanners(new ResourcesScanner(), new SubTypesScanner()));

            Set<String> xsdFiles = reflections.getResources(Pattern.compile(".*\\.xsd"));
            Source[] schemasSource = new Source[xsdFiles.size()];
            List<String> xsdFilesList = new ArrayList<String>(xsdFiles);
            Collections.sort(xsdFilesList, new XsdForLscComparator());
            for(String schemaFile: xsdFilesList) {
                LOGGER.debug("Importing XML schema file: " + schemaFile);
                InputStream schemaStream = this.getClass().getClassLoader().getResourceAsStream(schemaFile);
                schemasSource[i++] = new StreamSource(schemaStream);
            }
            lscSchema = schemaFactory.newSchema(schemasSource);
            unmarshaller.setSchema( lscSchema );

        } catch (VerifyError e) {
            throw new LscConfigurationException(e.toString(), e);
        } catch (SAXException e) {
            throw new LscConfigurationException(e);
        }
        return unmarshaller;
    }

    private String getEnvInlinedContent(Map<String, String> env, String filename) throws LscConfigurationException{
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filename));
            String line = reader.readLine();
            StringWriter writer = new StringWriter();

            while (line != null) {
                writer.write(inlineEnvForLine(env, line) + System.lineSeparator());
                line = reader.readLine();
            }
            return writer.toString();
        } catch (Exception e) {
            throw new LscConfigurationException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOGGER.error("Error during closing the configuration file " + filename, e);
                }
            }
        }
    }

    private String inlineEnvForLine(Map<String, String> env, String line) {
        String resultString = line;

        for(String envVariableKey : env.keySet()) {
            String escapedValue = StringEscapeUtils.escapeXml(env.get(envVariableKey));
            resultString = resultString.replaceAll("\\$\\{\\Q" + envVariableKey + "\\E\\}", escapedValue);
        }

        return resultString;

    }

    /**
     * Dump the object to an XML file (by overriding if necessary)
     *
     * @param filename
     *            filename to write to
     * @param lscConf
     *            configuration object
     * @throws FileNotFoundException
     *             thrown if the file can not be accessed (either because of a
     *             misconfiguration or due to a rights issue)
     */
    public void saveConfiguration(String filename, Lsc lscConf)
            throws IOException {
        File existing = new File(filename);
        if(existing.exists()) {
            File backup = new File(existing + ".bak");
            if(backup.exists()) {
                backup.delete();
            }
            FileUtils.copyFile(existing, backup);
        }
        try {
            Marshaller marshaller = jaxbc.createMarshaller();
            marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
            marshaller.marshal(lscConf, new File(filename));
        } catch (JAXBException e) {
            LOGGER.error("Cannot save configuration file: " + e.toString(), e);
        }
    }
}

<fim_middle>