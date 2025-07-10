<filename>src/main/java/com/agiletec/apsadmin/system/services/shortcut/ShortcutDefDOM.java<fim_prefix>

/*
 *
 * Copyright 2013 Entando S.r.l. (http://www.entando.com) All rights reserved.
 *
 * This file is part of Entando software.
 * Entando is a free software;
 * You can redistribute it and/or modify it
 * under the terms of the GNU General Public License (GPL) as published by the Free Software Foundation; version 2.
 *
 * See the file License for the specific language governing permissions
 * and limitations under the License
 *
 *
 *
 * Copyright 2013 Entando S.r.l. (http://www.entando.com) All rights reserved.
 *
 */
package com.agiletec.apsadmin.system.services.shortcut;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import com.agiletec.aps.system.ApsSystemUtils;
import com.agiletec.aps.system.exception.ApsSystemException;
import com.agiletec.apsadmin.system.services.shortcut.model.AbstractBaseBean;
import com.agiletec.apsadmin.system.services.shortcut.model.MenuSection;
import com.agiletec.apsadmin.system.services.shortcut.model.Shortcut;

/**
 * @author E.Santoboni
 */
public class ShortcutDefDOM {

    public ShortcutDefDOM(String xmlText, String definitionPath) throws ApsSystemException {
        this.validate(xmlText, definitionPath);
        this.decodeDOM(xmlText);
    }

    private void validate(String xmlText, String definitionPath) throws ApsSystemException {
        SchemaFactory factory =
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        <fim_suffix>
        InputStream schemaIs = null;
        InputStream xmlIs = null;
        try {
            schemaIs = this.getClass().getResourceAsStream("shortcuts-2.2.xsd");
            Source schemaSource = new StreamSource(schemaIs);
            Schema schema = factory.newSchema(schemaSource);
            Validator validator = schema.newValidator();
            xmlIs = new ByteArrayInputStream(xmlText.getBytes("UTF-8"));
            Source source = new StreamSource(xmlIs);
            validator.validate(source);
            ApsSystemUtils.getLogger().info("Valid Shortcut definition : " + definitionPath);
        } catch (Throwable t) {
            String message = "Error validating Shortcut definition : " + definitionPath;
            ApsSystemUtils.logThrowable(t, this, "this", message);
            throw new ApsSystemException(message, t);
        } finally {
            try {
                if (null != schemaIs) schemaIs.close();
                if (null != xmlIs) xmlIs.close();
            } catch (IOException e) {
                ApsSystemUtils.logThrowable(e, this, "this");
            }
        }
    }

    public Map<String, Shortcut> getShortcuts() {
        Map<String, Shortcut> shortcuts = new HashMap<String, Shortcut>();
        Element shortcutsElement = this._doc.getRootElement().getChild(SHORTCUTS_ELEMENT_NAME);
        if (null == shortcutsElement) {
            return shortcuts;
        }
        List<Element> shortcutElements = shortcutsElement.getChildren();
        for (int i=0; i<shortcutElements.size(); i++) {
            Element shortcutElement = shortcutElements.get(i);
            String id = shortcutElement.getAttributeValue(SHORTCUT_ID_ATTRIBUTE_NAME);
            Shortcut shortcut = new Shortcut(id);
            shortcut.setRequiredPermission(shortcutElement.getAttributeValue(SHORTCUT_REQ_PERMISSION_ATTRIBUTE_NAME));
            shortcut.setMenuSectionCode(shortcutElement.getAttributeValue(SHORTCUT_MENU_SECTION_ATTRIBUTE_NAME));
            shortcut.setSource(shortcutElement.getAttributeValue(SHORTCUT_SOURCE_ATTRIBUTE_NAME));
            this.extractDescriptions(shortcutElement, shortcut);
            Element urlElement = shortcutElement.getChild(SHORTCUT_URL_ELEMENT_NAME);
            if (null != urlElement) {
                List<Element> paramElements = urlElement.getChildren(PARAM_ELEMENT_NAME);
                for (int j=0; j<paramElements.size(); j++) {
                    Element paramElement = paramElements.get(j);
                    String name = paramElement.getAttributeValue(PARAM_NAME_ATTRIBUTE_NAME);
                    String value = paramElement.getText();
                    if (name.equals(NAMESPACE_PARAM_NAME)) {
                        shortcut.setNamespace(value);
                    } else if (name.equals(ACTIONNAME_PARAM_NAME)) {
                        shortcut.setActionName(value);
                    } else {
                        if (null == shortcut.getParameters()) {
                            shortcut.setParameters(new HashMap<String, Object>());
                        }
                        shortcut.getParameters().put(name, value);
                    }
                }
            }
            shortcuts.put(shortcut.getId(), shortcut);
        }
        return shortcuts;
    }

    public Map<String, MenuSection> getSectionMenus() {
        Map<String, MenuSection> menuSections = new HashMap<String, MenuSection>();
        Element menuSectionsElement = this._doc.getRootElement().getChild(MENUSECTIONS_ELEMENT_NAME);
        if (null == menuSectionsElement) {
            return menuSections;
        }
        List<Element> menuSectionElements = menuSectionsElement.getChildren();
        for (int i=0; i<menuSectionElements.size(); i++) {
            Element menuSectionElement = menuSectionElements.get(i);
            String id = menuSectionElement.getAttributeValue(MENUSECTION_ID_ATTRIBUTE_NAME);
            MenuSection menuSection = new MenuSection(id);
            this.extractDescriptions(menuSectionElement, menuSection);
            menuSections.put(menuSection.getId(), menuSection);
        }
        return menuSections;
    }

    private void extractDescriptions(Element element, AbstractBaseBean bean) {
        Element descrElement = element.getChild("description");
        if (null != descrElement) {
            bean.setDescription(descrElement.getText());
            bean.setDescriptionKey(descrElement.getAttributeValue("key"));
        }
        Element longdescElement = element.getChild("longdesc");
        if (null != longdescElement) {
            bean.setLongDescription(longdescElement.getText());
            bean.setLongDescriptionKey(longdescElement.getAttributeValue("key"));
        }
    }

    public String getXMLDocument(){
        XMLOutputter out = new XMLOutputter();
        Format format = Format.getPrettyFormat();
        format.setIndent("\t");
        out.setFormat(format);
        return out.outputString(_doc);
    }

    private void decodeDOM(String xmlText) throws ApsSystemException {
        SAXBuilder builder = new SAXBuilder();
        builder.setValidation(false);
        StringReader reader = new StringReader(xmlText);
        try {
            this._doc = builder.build(reader);
        } catch (Throwable t) {
            ApsSystemUtils.getLogger().error("Error while parsing: " + t.getMessage());
            throw new ApsSystemException("Error detected while parsing the XML", t);
        }
    }

    private Document _doc;

    public static final String ROOT_ELEMENT_NAME = "";
    public static final String SHORTCUTS_ELEMENT_NAME = "shortcuts";
    public static final String SHORTCUT_ID_ATTRIBUTE_NAME = "id";
    public static final String SHORTCUT_REQ_PERMISSION_ATTRIBUTE_NAME = "requiredPermission";
    public static final String SHORTCUT_MENU_SECTION_ATTRIBUTE_NAME = "menusection";
    public static final String SHORTCUT_SOURCE_ATTRIBUTE_NAME = "source";
    public static final String SHORTCUT_URL_ELEMENT_NAME = "url";
    public static final String PARAM_ELEMENT_NAME = "param";
    public static final String PARAM_NAME_ATTRIBUTE_NAME = "name";
    public static final String NAMESPACE_PARAM_NAME = "namespace";
    public static final String ACTIONNAME_PARAM_NAME = "actionName";

    public static final String MENUSECTIONS_ELEMENT_NAME = "menusections";
    public static final String MENUSECTION_ID_ATTRIBUTE_NAME = "id";

}
<fim_middle>