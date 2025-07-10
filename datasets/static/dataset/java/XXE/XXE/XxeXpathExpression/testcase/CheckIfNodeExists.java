<filename>src/main/java/com/howtodoinjava/xml/CheckIfNodeExists.java<fim_prefix>

package com.howtodoinjava.xml;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class CheckIfNodeExists {

    public static void main(String[] args) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        <fim_suffix>
        Document doc = builder.parse("employees.xml");

        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();

        //1
        XPathExpression expr = xpath.compile("//employees/employee/@id");
        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        if(nodes.getLength() > 0) {
            System.out.println("Attribute or Node Exists");
        } else {
            System.out.println("Attribute or Node Does Not Exist");
        }

        //2
        Double count = (Double) result;
        if(count > 0) {
            System.out.println("Attribute or Node Exists");
        } else {
            System.out.println("Attribute or Node Does Not Exist");
        }

    }
}

<fim_middle>