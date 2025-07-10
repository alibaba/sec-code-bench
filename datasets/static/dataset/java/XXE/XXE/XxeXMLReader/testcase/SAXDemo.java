<filename>Ch02/SAXDemo/v2/SAXDemo.java<fim_prefix>

import java.io.FileReader;
import java.io.IOException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import org.xml.sax.helpers.XMLReaderFactory;

public class SAXDemo
{
    public static void main(String[] args)
    {
        if (args.length < 1 || args.length > 2)
        {
            System.err.println("usage: java SAXDemo xmlfile [v]");
            return;
        }
        try
        {
            XMLReader xmlr = XMLReaderFactory.createXMLReader();
            if (args.length == 2 && args[1].equals("v"))
                xmlr.setFeature("http://xml.org/sax/features/validation", true);
            xmlr.setFeature("http://xml.org/sax/features/namespace-prefixes",
                    true);
            Handler handler = new Handler();
            <fim_suffix>
            xmlr.setErrorHandler(handler);
            xmlr.setProperty("http://xml.org/sax/properties/lexical-handler",
                    handler);
            xmlr.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                    "http://www.w3.org/2001/XMLSchema");
            xmlr.parse(new InputSource(new FileReader(args[0])));
        }
        catch (IOException ioe)
        {
            System.err.println("IOE: " + ioe);
        }
        catch (SAXException saxe)
        {
            System.err.println("SAXE: " + saxe);
        }
    }
}
<fim_middle>