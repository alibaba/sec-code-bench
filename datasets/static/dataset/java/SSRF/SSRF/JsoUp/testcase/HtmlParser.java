<filename>bigdata_tools-master/yongqing-bigdata-tools/yongqing-bigdata-tools-common/src/main/java/com/yongqing/common/bigdata/tool/HtmlParser.java<fim_prefix>

package com.yongqing.common.bigdata.tool;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;

/**
 * HTML  解析工具   https://jsoup.org/cookbook/input/load-document-from-url
 */
public class HtmlParser {
    public static Document getDocument(File htmlFile, String charsetName, String baseUri) throws Exception {
        return Jsoup.parse(htmlFile, charsetName, baseUri);
    }

    public static Document getDocument(String htmlString) throws Exception {
        return Jsoup.parse(htmlString);
    }

    public static Document getDocumentByGetUrl(String url) throws Exception {
        return getConnect(url).get();
    }

    public static Connection getConnect(String url) {
        
<fim_suffix>
        
    }

    public static Document getDocumentByPostUrl(String url) throws Exception {
        return getConnect(url).post();
    }

    public static Document getDocumentByPostUrl(String url, String charsetName) throws Exception {
        return getConnect(url).postDataCharset(charsetName).post();
    }


    public static Element getElementById(Document doc, String elementId) throws Exception {

        return doc.getElementById(elementId);
    }

    public static Elements getElementsByClass(Document doc, String elementId) throws Exception {

        return doc.getElementsByClass(elementId);
    }

    public static Elements getElementsByTag(Document doc, String elementId) throws Exception {

        return doc.getElementsByTag(elementId);
    }

    public static Elements getElementsByAttribute(Document doc, String elementId) throws Exception {

        return doc.getElementsByAttribute(elementId);
    }

    public static Elements getAllElements(Document doc, String baseUri) throws Exception {

        return doc.getAllElements();
    }


    public static Element getElementById(File htmlFile, String charsetName, String baseUri, String elementId) throws Exception {
        Document doc = getDocument(htmlFile, charsetName, baseUri);
        return doc.getElementById(elementId);
    }

    public static Elements getElementsByClass(File htmlFile, String charsetName, String baseUri, String elementId) throws Exception {
        Document doc = getDocument(htmlFile, charsetName, baseUri);
        return doc.getElementsByClass(elementId);
    }

    public static Elements getElementsByTag(File htmlFile, String charsetName, String baseUri, String elementId) throws Exception {
        Document doc = getDocument(htmlFile, charsetName, baseUri);
        return doc.getElementsByTag(elementId);
    }

    public static Elements getElementsByAttribute(File htmlFile, String charsetName, String baseUri, String elementId) throws Exception {
        Document doc = getDocument(htmlFile, charsetName, baseUri);
        return doc.getElementsByAttribute(elementId);
    }

    public static Elements getAllElements(File htmlFile, String charsetName, String baseUri) throws Exception {
        Document doc = getDocument(htmlFile, charsetName, baseUri);
        return doc.getAllElements();
    }

}
<fim_middle>