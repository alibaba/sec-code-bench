<filename>bw-maven-plugin/src/main/java/fr/fastconnect/factory/tibco/bw/maven/source/ProcessModel.java<fim_prefix>

/**
 * (C) Copyright 2011-2015 FastConnect SAS
 * (http://www.fastconnect.fr/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.fastconnect.factory.tibco.bw.maven.source;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.tibco.xmlns.bw.process._2003.ObjectFactory;
import com.tibco.xmlns.bw.process._2003.ProcessDefinition;

/**
 * <p>
 * This class will<ul>
 * <li>unmarshall XML file of the schema with
 * "http://xmlns.tibco.com/bw/process/2003" namespace to JAXB objects.
 * </li>
 * <li>
 * marshall the {@link ProcessDefinition} object back to XML file with the same
 * schema.
 * </li>
 * </ul>
 * </p>
 *
 * @author Mathieu Debove
 *
 */
public class ProcessModel {
//	private static final String PROCESS_MODEL_NAMESPACE = "http://xmlns.tibco.com/bw/process/2003";

    private ProcessDefinition process;
    public ProcessDefinition getProcess() {
        return process;
    }

    private JAXBContext jaxbContext;
    private File xmlFile;

    public ProcessModel(File xmlFile) throws JAXBException {
        this.xmlFile = xmlFile;
        initProcessModel();
    }

    /**
     * <p>
     * This will initialize the {@link ProcessDefinition} object which is a JAXB
     * representation of the "ProcessDefinition" root-element of TIBCO processes
     * files using the schema with "http://xmlns.tibco.com/bw/process/2003"
     * namespace.
     * </p>
     *
     * @throws JAXBException
     */
    private void initProcessModel() throws JAXBException {
        <fim_suffix>
        jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        Object o =  jaxbUnmarshaller.unmarshal(xmlFile);
        this.process = (ProcessDefinition) o;
    }

    /**
     * <p>
     * This will marshall the object back to the XML file.
     * </p>
     *
     * @throws JAXBException
     */
    public void save() throws JAXBException {
        Marshaller m = jaxbContext.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(process, xmlFile);
    }
}

<fim_middle>