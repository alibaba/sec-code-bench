<filename>pdt.connector/src/org/cs3/pdt/connector/registry/DefaultSAXPrologProcessRegistry.java<fim_prefix>

/*****************************************************************************
 * This file is part of the Prolog Development Tool (PDT)
 *
 * Author: Lukas Degener (among others)
 * WWW: http://sewiki.iai.uni-bonn.de/research/pdt/start
 * Mail: pdt@lists.iai.uni-bonn.de
 * Copyright (C): 2004-2012, CS Dept. III, University of Bonn
 *
 * All rights reserved. This program is  made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 ****************************************************************************/
package org.cs3.pdt.connector.registry;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.cs3.pdt.connector.subscription.PersistableSubscription;
import org.cs3.pdt.connector.subscription.Subscription;
import org.cs3.prolog.connector.common.Debug;
import org.cs3.prolog.connector.common.Util;
import org.eclipse.core.runtime.Platform;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class DefaultSAXPrologProcessRegistry extends DefaultPrologProcessRegistry {

    @Override
    public void load(Reader reader) throws IOException {
        //Element cpElement;
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            <fim_suffix>
            //parser.setProperty("http://xml.org/sax/features/namespaces", new Boolean(true));
            //boolean b = parser.isNamespaceAware();
            SAXParser parser = factory.newSAXParser();
            parser.parse(new InputSource(reader),new RegistryHandler());

        } catch (SAXException e) {
            // TODO: changed this to new IOException(e) once Java 6 is supported on all target platforms
            Debug.report(e);
            throw new IOException(e.getLocalizedMessage());
        } catch (ParserConfigurationException e) {
            // TODO: changed this to new IOException(e) once Java 6 is supported on all target platforms
            Debug.report(e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            reader.close();
        }
    }

    class RegistryHandler extends DefaultHandler {

        public RegistryHandler() {

        }

        PersistableSubscription subscription;

        @Override
        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) throws SAXException {
            if (qName.equals("subscription")) {

                String bundle = attributes.getValue("bundle");
                String className = attributes.getValue("class");
                int count = attributes.getLength();
                HashMap<String, String> params = new HashMap<String, String>();
                for (int j = 0; j < count; j++) {
                    String value = attributes.getValue(j);
                    String name = attributes.getLocalName(j);
                    params.put(name, value);
                }

                Class<?> clazz;
                try {
                    clazz = Platform.getBundle(bundle).loadClass(className);
                    PersistableSubscription subscription = (PersistableSubscription) clazz
                            .newInstance();
                    subscription.restoreState(params);
                    addSubscription(subscription);
                } catch (ClassNotFoundException e) {
                    Debug.rethrow(e);
                } catch (IllegalAccessException e) {
                    Debug.rethrow(e);
                } catch (InstantiationException e) {
                    Debug.rethrow(e);
                }
            }

        }
    }



    @Override
    public void save(Writer w) throws IOException {
        w.write("<registry>\n");
        try {

            for (Subscription s : getAllSubscriptions()) {


                PersistableSubscription ps=null;
                if (s instanceof PersistableSubscription) {

                    ps = (PersistableSubscription) s;
                }
                if(ps!=null && ps.isPersistent()){
                    Map<String, String> m = ps.saveState();
                    m.put("class",ps.getClass().getName());
                    m.put("bundle",ps.getHostId());
                    w.write("   <subscription\n");
                    try{
                        for (Iterator<String> jt = m.keySet().iterator(); jt.hasNext();) {
                            String key = Util.escape(jt.next());
                            String value = Util.escape(m.get(key));
                            w.write("      "+key+"=\""+value+"\"\n");
                        }
                    }
                    finally{
                        w.write("   />\n");
                    }
                }
            }
        } finally {
            w.write("</registry>\n");
        }
    }



}



<fim_middle>