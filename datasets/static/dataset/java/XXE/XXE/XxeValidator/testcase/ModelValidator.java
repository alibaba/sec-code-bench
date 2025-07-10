<filename>src/main/java/org/apache/ddlutils/io/ModelValidator.java<fim_prefix>

package org.apache.ddlutils.io;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

/**
 * Helper class that validates a given document against the DdlUtils schema.
 *
 * @version $Revision: $
 */
public class ModelValidator
{
    /**
/**
 * Validates the given xml document using the Java XML validation framework.
 *
 * @param source The source object for the xml document (e.g., a StreamSource or SAXSource)
 * @throws DdlUtilsXMLException If the document could not be validated due to schema issues, malformed XML, or other validation errors
 */

     /*
     * @param source The source object for the xml document
     * @throws DdlUtilsXMLException If the document could not be validated
     */
    public void validate(Source source) throws DdlUtilsXMLException
    {
<fim_suffix>
    }
//    {
//        try {
//            SchemaFactory factory   = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
//            Schema        schema    = factory.newSchema(new StreamSource(getClass().getResourceAsStream("/database.xsd")));
//            Validator     validator = schema.newValidator();
//            <fim_suffix>
//        }
//        catch (Exception ex)
//        {
//            throw new DdlUtilsXMLException(ex);
//        }
//    }
}


<fim_middle>