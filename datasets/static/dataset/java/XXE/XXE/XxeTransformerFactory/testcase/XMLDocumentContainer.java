<filename>src/main/java/org/apache/commons/jxpath/XMLDocumentContainer.java<fim_prefix>

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jxpath;

import java.net.URL;
import java.util.Objects;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;

import org.apache.commons.jxpath.xml.DocumentContainer;

/**
 * An XML document container reads and parses XML only when it is
 * accessed.  JXPath traverses Containers transparently -
 * you use the same paths to access objects in containers as you
 * do to access those objects directly.  You can create
 * XMLDocumentContainers for various XML documents that may or
 * may not be accessed by XPaths.  If they are, they will be automatically
 * read, parsed and traversed. If they are not - they won't be
 * read at all.
 *
 * @deprecated 1.1 Please use {@link DocumentContainer}
 */
@Deprecated
public class XMLDocumentContainer implements Container {

    private static final long serialVersionUID = 1L;
    private DocumentContainer delegate;
    private Object document;
    private URL xmlURL;
    private Source source;

    /**
     * Create a new XMLDocumentContainer.
     * @param xmlURL a URL for an XML file. Use getClass().getResource(resourceName)
     *               to load XML from a resource file.
     */
    public XMLDocumentContainer(final URL xmlURL) {
        this.xmlURL = xmlURL;
        delegate = new DocumentContainer(xmlURL);
    }

    /**
     * Create a new XMLDocumentContainer.
     * @param source XML source
     */
    public XMLDocumentContainer(final Source source) {
        this.source = Objects.requireNonNull(source);
    }

    /**
     * Reads XML, caches it internally and returns the Document.
     * @return Object value
     */
    @Override
    public Object getValue() {
        if (document == null) {
            try {
                if (source != null) {
                    final DOMResult result = new DOMResult();
                    final Transformer trans =
                            TransformerFactory.newInstance().newTransformer();
                    <fim_suffix>
                    trans.transform(source, result);
                    document = result.getNode();
                }
                else {
                    document = delegate.getValue();
                }
            }
            catch (final Exception ex) {
                throw new JXPathException(
                        "Cannot read XML from: "
                                + (xmlURL != null
                                ? xmlURL.toString()
                                : source != null
                                ? source.getSystemId()
                                : "<<undefined source>>"),
                        ex);
            }
        }
        return document;
    }

    /**
     * Throws an UnsupportedOperationException
     * @param value to set
     */
    @Override
    public void setValue(final Object value) {
        throw new UnsupportedOperationException();
    }
}


<fim_middle>