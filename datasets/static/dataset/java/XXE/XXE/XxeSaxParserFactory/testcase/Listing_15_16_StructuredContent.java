<filename>src/main/java/com/itextpdf/samples/book/part4/chapter15/Listing_15_16_StructuredContent.java<fim_prefix>

package com.itextpdf.samples.book.part4.chapter15;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.tagging.PdfStructTreeRoot;
import com.itextpdf.kernel.pdf.tagging.StandardRoles;
import com.itextpdf.kernel.pdf.tagutils.TagTreePointer;
import com.itextpdf.layout.Document;

import java.io.File;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Listing_15_16_StructuredContent {
    public static final String DEST
            = "./target/book/part4/chapter15/Listing_15_16_StructuredContent.pdf";
    public static String RESOURCE
            = "./src/main/resources/xml/moby.xml";

    public static void main(String args[]) throws IOException, SQLException, ParserConfigurationException, SAXException {
        File file = new File(DEST);
        file.getParentFile().mkdirs();

        new Listing_15_16_StructuredContent().manipulatePdf(DEST);
    }

    public void manipulatePdf(String dest) throws IOException, SQLException, ParserConfigurationException, SAXException {
        //Initialize document
        PdfDocument pdfDoc = new PdfDocument(new PdfWriter(dest));
        Document doc = new Document(pdfDoc);

        pdfDoc.setDefaultPageSize(PageSize.A5);
        pdfDoc.setTagged();

        PdfStructTreeRoot root = pdfDoc.getStructTreeRoot();
        root.addRoleMapping("chapter", StandardRoles.SECT);
        root.addRoleMapping("title", StandardRoles.H);
        root.addRoleMapping("para", StandardRoles.P);

        // See TaggingSamples for more information
        TagTreePointer autoTaggingPointer = pdfDoc.getTagStructureContext().getAutoTaggingPointer();
        // create a new tag, which will be a kid of the root element, and move to it
        autoTaggingPointer.addTag("chapter");

        SAXParserFactory factory = SAXParserFactory.newInstance();
        <fim_suffix>
        List<String> roles = new ArrayList<String>();
        parser.parse(
                new InputSource(new FileInputStream(RESOURCE)),
                new Listing_15_17_StructureParser(roles));
        parser.parse(
                new InputSource(new FileInputStream(RESOURCE)),
                new Listing_15_18_ContentParser(doc, roles));
        doc.close();
    }
}
<fim_middle>
