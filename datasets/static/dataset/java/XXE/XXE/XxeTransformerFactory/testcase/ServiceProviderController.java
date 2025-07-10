<filename>demo/saml-sp/src/main/java/demo/springone2018/saml/web/ServiceProviderController.java<fim_prefix>

/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package demo.springone2018.saml.web;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml.SamlAuthentication;
import org.springframework.security.saml.SamlTransformer;
import org.springframework.security.saml.saml2.attribute.Attribute;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import static java.util.Collections.emptyList;

@Controller
public class ServiceProviderController {

    private static final Log logger =LogFactory.getLog(ServiceProviderController.class);
    private SamlTransformer transformer;

    @Autowired
    public ServiceProviderController setTransformer(SamlTransformer transformer) {
        this.transformer = transformer;
        return this;
    }

    @RequestMapping(value = {"/", "/index", "/logged-in"})
    public String home(Model model) {
        logger.info("Sample SP Application - You are logged in!");
        populateModel(model);
        return "logged-in";
    }

    private void populateModel(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Attribute> attributes = emptyList();
        String xml = null;
        if (authentication instanceof SamlAuthentication) {
            SamlAuthentication sa = (SamlAuthentication)authentication;
            attributes = sa.getAssertion().getAttributes();
            xml = transformer.toXml(sa.getAssertion());
        }
        model.addAttribute("attributes", attributes);
        model.addAttribute("xml", prettyPrint(xml));
    }


    private String prettyPrint(String xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xml));
            Document doc = db.parse(is);

            <fim_suffix>
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(doc);
            transformer.transform(source, result);
            return result.getWriter().toString();
        } catch (Exception x) {
            return xml;
        }
    }

}

<fim_middle>