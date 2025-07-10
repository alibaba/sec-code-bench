<filename>src/main/java/enviromine/client/gui/menu/update/WordPressParser.java<fim_prefix>

package enviromine.client.gui.menu.update;
import java.io.StringReader;

import org.apache.commons.lang3.StringEscapeUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class WordPressParser extends DefaultHandler
{

    public WordPressParser ()
    {
        super();
    }

    public static void main (String page)	throws Exception
    {
        XMLReader xr = XMLReaderFactory.createXMLReader();
        WordPressParser handler = new WordPressParser();
        <fim_suffix>

        // Parse each file provided on the
        // command line.

        //FileReader r = new FileReader(page);
        xr.parse(new InputSource(new StringReader(page)));



    }
    boolean item = false;
    boolean bTitle = false;
    boolean bLink = false;
    boolean bCreator = false;
    boolean bPubDate = false;
    boolean bDescription = false;

    public void startElement (String uri, String name,  String qName, Attributes atts)
    {
        if(qName.equalsIgnoreCase("item"))
        {
            item = true;
        }

        if(qName.equalsIgnoreCase("title") && item)
        {
            bTitle = true;
        }
        else if(qName.equalsIgnoreCase("link") && item)
        {
            bLink = true;
        }
        else if(qName.equalsIgnoreCase("dc:creator") && item)
        {
            bCreator = true;
        }
        else if(qName.equalsIgnoreCase("pubDate") && item)
        {
            bPubDate = true;
        }
        else if(qName.equalsIgnoreCase("content:encoded") && item)
        {
            bDescription = true;
        }

    }

    int count = 0;
    public void endElement (String uri, String name, String qName)
    {
        if(qName.equalsIgnoreCase("item") && count <= 10 && item)
        {
            item = false;


            WordPressPost post = new WordPressPost(title, description, link, pubDate, creator);

            WordPressPost.Posts.add(count, post);

            title = ""; link = ""; creator = ""; pubDate = ""; description = "";

            count++;
        }

    }


    private String title;
    private String link;
    private String creator;
    private String pubDate;
    private String description;

    public void characters (char ch[], int start, int length)
    {

        if(bTitle)
        {
            String vart = new String(ch, start, length);
            title = vart;
            bTitle = false;
        }
        else if(bLink)
        {
            String vart = new String(ch, start, length);
            link = vart;
            bLink = false;
        }
        else if (bCreator)
        {
            String vart = new String(ch, start, length);
            creator = vart;
            bCreator = false;
        }
        else if(bPubDate)
        {
            String vart = new String(ch, start, length);
            vart =StringEscapeUtils.unescapeXml(vart);
            vart = vart.toString().replaceAll("\\<.*?>","");
            vart =StringEscapeUtils.unescapeHtml4(vart);
            vart = vart.toString().replaceAll("\u00a0","");
            pubDate = vart;
            bPubDate = false;
        }
        else if(bDescription)
        {
            String vart = new String(ch, start, length);
            vart =StringEscapeUtils.unescapeXml(vart);
            vart = vart.toString().replaceAll("\\<.*?>","");
            vart =StringEscapeUtils.unescapeHtml4(vart);
            vart = vart.toString().replaceAll("\u00a0","");
            vart = vart.toString().replaceAll("Filed under:.*","");
            description = vart;
            bDescription = false;
        }

    }
}
<fim_middle>