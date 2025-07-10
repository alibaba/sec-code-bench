<filename>Bypassing/Scripts/ParsingHTML/ParsingHTML.java<fim_prefix>

package parsinghtml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 *
 * @author Alaa
 */

/*
 TO DO: 
 1 - Identifying Entry Points for User Inputs
        Input handling contexts:
            - HTML element content
            - HTML attribute value
            - URL query value
            - CSS value
            - JavaScript value
            - What else ? 
 2 - Identifying The Reflection of User Input From The previus Entry Points
 3 - Crafting The payload 
 */
public class ParsingHTML {

    // in case you need to parse a local HTML file
    File in;
    Document doc;

    ParsingHTML(String URL, String filePath) throws IOException {
    // set null for the parameter that you don't need
        if (URL != null) {
            this.connectToURL(URL);
        } // end if 

        if (filePath != null) {
            this.setFile(filePath);
            this.setDoc(this.getFile());
        } // end if 
    } // end ParsingHTML()

    public static void main(String[] args) throws IOException {

        // initializing labs names because I'm too lazy again 
        String[] labs = new String[11];
        for (int i = 0; i < labs.length; i++) {
            labs[i] = "http://10.10.10.10:8000/" + "XSSLab" + (i + 1) + ".html";
        } // end for 

        // checking labs files names
       /* System.out.println("Labs Names: ");
        for (int i = 0; i < labs.length; i++) {
            System.out.println(labs[i]);
        } // end for */

        // creating array of objects, each object represents one lab (one html file)
        ParsingHTML[] objects = new ParsingHTML[11];

        // initializing objects
        for (int i = 0; i < objects.length; i++) {
            objects[i] = new ParsingHTML(labs[i], null);
        } // end for

        for (int i = 0; i < objects.length; i++) {
            System.out.println("-== Lab title: ==-");
            System.out.println(objects[i].getTitle());
            System.out.println("-== Getting all Forms Tags ==-");
            Elements element = objects[i].getFormsTags();
            System.out.println(element);
            System.out.println("-===================================-");
        } // end for
    } // end main()

    private void setFile(String filePath) {
        this.in = new File(filePath);
    } // end setFile()

    private File getFile() {
        return this.in;
    } // end getFile()

    private void setDoc(File in) throws IOException {
        this.doc = Jsoup.parse(in, "utf-8");
    } // end setDoc()

    private Document getDoc() throws IOException {
        return this.doc;
    } // end getDoc()

    // Creates a new Connection to a URL. Use to fetch and parse an HTML page. 
    private void connectToURL(String URL) throws IOException {

<fim_suffix>

        this.doc = Jsoup.connect(URL).get();
    } // end connectToURL()

    private void checkFileContent(File in) throws FileNotFoundException {
        // check the content of the file 
        Scanner input = new Scanner(in);
        while (input.hasNextLine()) {
        System.out.println(input.nextLine());
        }
    } // end checkFileContent()

    private Elements getFormsTags() throws IOException {
        Elements element = this.getDoc().getElementsByTag("form");
        return element;
    } // end getFormsTags()

    private Elements getInputsTags() throws IOException {
        Elements element = this.getDoc().getElementsByTag("input");
        return element;
    } // end getInputsTags() 

    private String getTitle() throws IOException {
        String title = this.getDoc().title();
        return title;
    } // end getTitle()
} // end class
<fim_middle>