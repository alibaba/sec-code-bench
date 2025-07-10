<filename>src/main/resources/evaluation/LocalEntityDemo.java<fim_prefix>

package com.leadroyal.xxe;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class LocalEntityDemo {
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        <fim_suffix>
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.parse(new File("local.xml"));
        NodeList nodes = doc.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                System.out.println(nodes.item(i).getTextContent());
            }
        }
    }
}
<fim_middle>