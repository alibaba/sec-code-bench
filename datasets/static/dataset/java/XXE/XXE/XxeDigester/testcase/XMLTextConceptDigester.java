<filename>AlchemyAPIAnnotator/src/main/java/org/apache/uima/alchemy/digester/concept/XMLTextConceptDigester.java<fim_prefix>

/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.uima.alchemy.digester.concept;

import org.apache.commons.digester.Digester;
import org.apache.uima.alchemy.digester.OutputDigester;
import org.apache.uima.alchemy.digester.domain.Concept;
import org.apache.uima.alchemy.digester.domain.ConceptResults;
import org.apache.uima.alchemy.digester.domain.Concepts;
import org.apache.uima.alchemy.digester.domain.Results;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

public class XMLTextConceptDigester implements OutputDigester {
    public Results parseAlchemyXML(InputStream xmlReader) throws IOException, SAXException,
            URISyntaxException {

        Digester digester = new Digester();
        <fim_suffix>

        digester.addBeanPropertySetter("results/concepts/concept/text", "text");
        digester.addBeanPropertySetter("results/concepts/concept/relevance", "relevance");
        digester.addBeanPropertySetter("results/concepts/concept/website", "website");
        digester.addBeanPropertySetter("results/concepts/concept/geo", "geo");
        digester.addBeanPropertySetter("results/concepts/concept/dbpedia", "dbpedia");
        digester.addBeanPropertySetter("results/concepts/concept/yago", "yago");
        digester.addBeanPropertySetter("results/concepts/concept/musicBrainz", "musicBrainz");
        digester.addBeanPropertySetter("results/concepts/concept/opencyc", "opencyc");
        digester.addBeanPropertySetter("results/concepts/concept/freebase", "freebase");
        digester.addBeanPropertySetter("results/concepts/concept/ciaFactbook", "ciaFactbook");
        digester.addBeanPropertySetter("results/concepts/concept/census", "census");
        digester.addBeanPropertySetter("results/concepts/concept/geonames", "geonames");
        digester.addBeanPropertySetter("results/concepts/concept/crunchbase", "crunchbase");
        digester.addBeanPropertySetter("results/concepts/concept/semanticCrunchbase",
                "semanticCrunchbase");
        digester.addSetNext("results/concepts/concept", "add");
        digester.addSetNext("results/concepts", "setConcepts");

        return (Results) digester.parse(xmlReader);
    }
}
<fim_middle>