<filename>saiku-core/saiku-web/src/main/java/org/saiku/web/export/FoConverter.java<fim_prefix>

package org.saiku.web.export;

import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

class FoConverter {

    public static Document getFo(Document xmlDoc) {
        try {
            return xml2Fo(xmlDoc);
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private static Document xml2Fo(Document xml) throws javax.xml.transform.TransformerException {
        DOMSource xmlDomSource = new DOMSource(xml);
        DOMResult domResult = new DOMResult();
        Transformer transformer = createTransformer();
        transformer.transform(xmlDomSource, domResult);
        return (org.w3c.dom.Document) domResult.getNode();
    }

    private static Transformer createTransformer() throws TransformerConfigurationException {
        TransformerFactory factory = TransformerFactory.newInstance();
        <fim_suffix>

        if (transformer == null) {
            System.out.println("Error creating transformer");
            throw new TransformerConfigurationException("Transformer is null");
        }
        return transformer;
    }

}

<fim_middle>