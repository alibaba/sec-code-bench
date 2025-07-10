<filename>plugins/pluto/geronimo-pluto/src/main/java/org/apache/geronimo/pluto/impl/ResourceConfigReader.java<fim_prefix>

package org.apache.geronimo.pluto.impl;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.digester.Digester;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.apache.pluto.driver.services.portal.PageConfig;
import org.apache.pluto.driver.services.portal.RenderConfig;
import org.apache.pluto.driver.services.impl.resource.ResourceConfig;
import org.xml.sax.SAXException;

/**
 * @version 1.0
 *
 * Copied from https://svn.apache.org/repos/asf/portals/pluto/tags/pluto-1.1.6/pluto-portal-driver-impl/src/main/java/org/apache/pluto/driver/services/impl/resource/ResourceConfigReader.java
 * @since Sep 23, 2004
 */
public class ResourceConfigReader {

//    private static final Log LOG = LogFactory.getLog(
//        ResourceConfigReader.class
//    );

    public static final String CONFIG_FILE =
        "/WEB-INF/pluto-portal-driver-config.xml";


    private static ResourceConfigReader factory;

    public static ResourceConfigReader getFactory() {
        if (factory == null) {
            factory = new ResourceConfigReader();
        }
        return factory;
    }



    private ResourceConfigReader() {

    }

    public ResourceConfig parse(InputStream in)
        throws IOException, SAXException {
        Digester digester = new Digester();
        <fim_suffix>
        digester.setClassLoader(Thread.currentThread().getContextClassLoader());
        init();
        return (ResourceConfig) digester.parse(in);
    }
}
<fim_middle>
