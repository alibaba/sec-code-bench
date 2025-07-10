<filename>sipXconfig/neoconf/src/org/sipfoundry/sipxconfig/setting/XmlModelBuilder.java<fim_prefix>

/*
 *
 *
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 *
 *
 */
package org.sipfoundry.sipxconfig.setting;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.digester.BeanPropertySetterRule;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.commons.digester.RuleSetBase;
import org.apache.commons.digester.SetNestedPropertiesRule;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sipfoundry.sipxconfig.setting.type.BooleanSetting;
import org.sipfoundry.sipxconfig.setting.type.EnumSetting;
import org.sipfoundry.sipxconfig.setting.type.FileSetting;
import org.sipfoundry.sipxconfig.setting.type.HostnameSetting;
import org.sipfoundry.sipxconfig.setting.type.IntegerSetting;
import org.sipfoundry.sipxconfig.setting.type.IpAddrSetting;
import org.sipfoundry.sipxconfig.setting.type.IpAddrWildCardSetting;
import org.sipfoundry.sipxconfig.setting.type.MultiEnumSetting;
import org.sipfoundry.sipxconfig.setting.type.PhonePadPinSetting;
import org.sipfoundry.sipxconfig.setting.type.RealSetting;
import org.sipfoundry.sipxconfig.setting.type.SettingType;
import org.sipfoundry.sipxconfig.setting.type.SipUriSetting;
import org.sipfoundry.sipxconfig.setting.type.StringSetting;
import org.sipfoundry.sipxconfig.setting.type.UsernameSequenceSetting;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Build a SettingModel object hierarchy from a model XML file.
 */
public class XmlModelBuilder implements ModelBuilder {
    private static final String ADD_SETTING_METHOD = "addSetting";
    private static final String EL_VALUE = "/value";
    private static final String EL_LABEL = "/label";
    private static final String REQUIRED = "required";
    private static final String EL_OPTION = "/option";
    private static final String ADD_ENUM_METHOD = "addEnum";

    private final File m_configDirectory;

    public XmlModelBuilder(File configDirectory) {
        m_configDirectory = configDirectory;
    }

    public XmlModelBuilder(String configDirectory) {
        this(new File(configDirectory));
    }

    public SettingSet buildModel(File modelFile) {
        FileInputStream is = null;
        try {
            is = new FileInputStream(modelFile);
            SettingSet model = buildModel(is, modelFile.getParentFile());
            ModelMessageSource messageSource = new ModelMessageSource(modelFile);
            model.setMessageSource(messageSource);
            return model;

        } catch (IOException e) {
            throw new RuntimeException("Cannot parse model definitions file " + modelFile.getPath(), e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private SettingSet buildModel(InputStream is, File baseSystemId) throws IOException {
        Digester digester = new Digester();
        <fim_suffix>
        digester.setValidating(false);
        EntityResolver entityResolver = new ModelEntityResolver(m_configDirectory, baseSystemId);
        digester.setEntityResolver(entityResolver);
        digester.push(new ConditionalSet());

        // keeps all types encountered during parsing
        SettingTypeIdRule typeIdRule = new SettingTypeIdRule();

        addSettingTypes(digester, "model/type/", typeIdRule);

        CollectionRuleSet collectionRule = new CollectionRuleSet();
        digester.addRuleSet(collectionRule);

        SettingRuleSet groupRule = new SettingRuleSet("*/group", ConditionalSet.class, typeIdRule);
        digester.addRuleSet(groupRule);

        SettingRuleSet settingRule = new SettingRuleSet("*/setting", ConditionalSettingImpl.class, typeIdRule);
        digester.addRuleSet(settingRule);

        try {
            return (SettingSet) digester.parse(is);
        } catch (SAXException se) {
            throw new RuntimeException("Could not parse model definition file", se);
        }
    }

    private static void addSettingTypes(Digester digester, String patternPrefix, SettingTypeIdRule typeIdRule) {
        digester.addRuleSet(new IntegerSettingRule(patternPrefix + "integer", typeIdRule));
        digester.addRuleSet(new RealSettingRule(patternPrefix + "real", typeIdRule));
        digester.addRuleSet(new StringSettingRule(patternPrefix + "string", typeIdRule));
        digester.addRuleSet(new EnumSettingRule(patternPrefix + "enum", typeIdRule));
        digester.addRuleSet(new MultiEnumSettingRule(patternPrefix + "multi-enum", typeIdRule));
        digester.addRuleSet(new BooleanSettingRule(patternPrefix + "boolean", typeIdRule));
        digester.addRuleSet(new FileSettingRule(patternPrefix + "file", typeIdRule));
        digester.addRuleSet(new SpecializedStringSettingRule(patternPrefix + "sip-uri", typeIdRule,
                SipUriSetting.class));
        digester.addRuleSet(new SpecializedStringSettingRule(patternPrefix + "ipaddr", typeIdRule,
                IpAddrSetting.class));
        digester.addRuleSet(new SpecializedStringSettingRule(patternPrefix + "ipaddrwildcard", typeIdRule,
                IpAddrWildCardSetting.class));
        digester.addRuleSet(new SpecializedStringSettingRule(patternPrefix + "hostname", typeIdRule,
                HostnameSetting.class));
        digester.addRuleSet(new SpecializedStringSettingRule(patternPrefix + "phonepadpin", typeIdRule,
                PhonePadPinSetting.class));
        digester.addRuleSet(new SpecializedStringSettingRule(patternPrefix + "username_sequence", typeIdRule,
                UsernameSequenceSetting.class));
    }

    static class AbstractSettingRuleSet extends RuleSetBase {
        private final String m_pattern;
        private final Class m_klass;

        public AbstractSettingRuleSet(String pattern, Class klass) {
            m_pattern = pattern;
            m_klass = klass;
        }

        @Override
        public void addRuleInstances(Digester digester) {
            digester.addObjectCreate(m_pattern, m_klass);
            digester.addSetProperties(m_pattern, "parent", null);
            final String[] properties = {
                    "/description", "/profileName", EL_LABEL
            };
            for (int i = 0; i < properties.length; i++) {
                digester.addBeanPropertySetter(m_pattern + properties[i]);
            }
        }

        protected String getPattern() {
            return m_pattern;
        }
    }

    static class CollectionRuleSet extends AbstractSettingRuleSet {
        public CollectionRuleSet() {
            super("*/collection", SettingArray.class);
        }

        @Override
        public void addRuleInstances(Digester digester) {
            super.addRuleInstances(digester);
            digester.addSetNext(getPattern(), ADD_SETTING_METHOD, Setting.class.getName());
        }
    }

    static class SettingRuleSet extends AbstractSettingRuleSet {
        private final SettingTypeIdRule m_typeIdRule;

        public SettingRuleSet(String pattern, Class klass, SettingTypeIdRule typeIdRule) {
            super(pattern, klass);
            m_typeIdRule = typeIdRule;
        }

        @Override
        public void addRuleInstances(Digester digester) {
            super.addRuleInstances(digester);
            digester.addRule(getPattern(), new CopyOfRule());
            digester.addRule(getPattern() + EL_VALUE, new BeanPropertyNullOnEmptyStringRule("value"));
            addSettingTypes(digester, getPattern() + "/type/", m_typeIdRule);
            digester.addSetNext(getPattern(), ADD_SETTING_METHOD, ConditionalSettingImpl.class.getName());
        }
    }

}
<fim_middle>