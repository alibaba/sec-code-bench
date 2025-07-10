<filename>plugins/myfaces/geronimo-myfaces/src/main/java/org/apache/geronimo/myfaces/FacesConfigDigester.java<fim_prefix>

package org.apache.geronimo.myfaces;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.digester.Digester;
import org.apache.myfaces.config.impl.digester.elements.AbsoluteOrdering;
import org.apache.myfaces.config.impl.digester.elements.Application;
import org.apache.myfaces.config.impl.digester.elements.Attribute;
import org.apache.myfaces.config.impl.digester.elements.Behavior;
import org.apache.myfaces.config.impl.digester.elements.ClientBehaviorRenderer;
import org.apache.myfaces.config.impl.digester.elements.ConfigOthersSlot;
import org.apache.myfaces.config.impl.digester.elements.Converter;
import org.apache.myfaces.config.impl.digester.elements.FacesConfig;
import org.apache.myfaces.config.impl.digester.elements.FacesConfigNameSlot;
import org.apache.myfaces.config.impl.digester.elements.Factory;
import org.apache.myfaces.config.impl.digester.elements.ListEntries;
import org.apache.myfaces.config.impl.digester.elements.LocaleConfig;
import org.apache.myfaces.config.impl.digester.elements.ManagedBean;
import org.apache.myfaces.config.impl.digester.elements.ManagedProperty;
import org.apache.myfaces.config.impl.digester.elements.MapEntries;
import org.apache.myfaces.config.impl.digester.elements.NavigationCase;
import org.apache.myfaces.config.impl.digester.elements.NavigationRule;
import org.apache.myfaces.config.impl.digester.elements.Ordering;
import org.apache.myfaces.config.impl.digester.elements.Property;
import org.apache.myfaces.config.impl.digester.elements.Redirect;
import org.apache.myfaces.config.impl.digester.elements.RenderKit;
import org.apache.myfaces.config.impl.digester.elements.Renderer;
import org.apache.myfaces.config.impl.digester.elements.ResourceBundle;
import org.apache.myfaces.config.impl.digester.elements.SystemEventListener;
import org.apache.myfaces.config.impl.digester.elements.ViewParam;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @version $Rev$ $Date$
 */
public class FacesConfigDigester {

    private Digester digester;

    private static final InputSource EMPTY_INPUT_SOURCE = new InputSource(new ByteArrayInputStream(new byte[0]));

    public FacesConfigDigester() {
        digester = new Digester();
        // TODO: validation set to false during implementation of 1.2
        <fim_suffix>
        digester.setEntityResolver(new EntityResolver() {

            @Override
            public InputSource resolveEntity(String arg0, String arg1) throws SAXException, IOException {
                return EMPTY_INPUT_SOURCE;
            }

        });
        //digester.setUseContextClassLoader(true);
        digester.setClassLoader(FacesConfig.class.getClassLoader());

        digester.addObjectCreate("faces-config", FacesConfig.class);
        // 2.0 specific start
        digester.addSetProperties("faces-config", "metadata-complete", "metadataComplete");
        digester.addSetProperties("faces-config", "version", "version");
        // 2.0 specific end
        // 2.0 config ordering name start
        digester.addCallMethod("faces-config/name", "setName", 0);
        digester.addObjectCreate("faces-config/ordering", Ordering.class);
        digester.addSetNext("faces-config/ordering", "setOrdering");
        digester.addObjectCreate("faces-config/ordering/before/name", FacesConfigNameSlot.class);
        digester.addSetNext("faces-config/ordering/before/name", "addBeforeSlot");
        digester.addCallMethod("faces-config/ordering/before/name", "setName", 0);
        digester.addObjectCreate("faces-config/ordering/before/others", ConfigOthersSlot.class);
        digester.addSetNext("faces-config/ordering/before/others", "addBeforeSlot");


        InputStream in = null;
        InputSource is = new InputSource(in);
        is.setSystemId(systemId);

        // Fix for http://issues.apache.org/jira/browse/MYFACES-236
        FacesConfig config = (FacesConfig) digester.parse(is);

        for (org.apache.myfaces.config.element.Application application : config.getApplications()) {
            for (org.apache.myfaces.config.element.LocaleConfig localeConfig : application.getLocaleConfig()) {
                if (!localeConfig.getSupportedLocales().contains(localeConfig.getDefaultLocale())) {
                    localeConfig.getSupportedLocales().add(localeConfig.getDefaultLocale());
                }
            }
        }
    }
}
<fim_middle>
