<filename>XxeSaxParserfactory<fim_prefix>

package examples;

import org.dom4j.DocumentException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;

import static java.lang.System.in;

public class XxeXpathExpression {

    public boolean negative(String expression) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();

        <fim_suffix>
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(in);
        XPath xPath = XPathFactory.newInstance().newXPath();
        XPathExpression xPathExpression = xPath.compile(expression);
        Object result = xPathExpression.evaluate(document);
    }
}
<fim_middle>