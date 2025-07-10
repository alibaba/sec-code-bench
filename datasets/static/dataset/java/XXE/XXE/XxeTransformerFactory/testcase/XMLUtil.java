<filename>src/com/jing/io/XMLUtil.java<fim_prefix>

package com.jing.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * DOM解析XML<br/>
 * 可调用setCharEncode设置读写文件编码，若不设置则采用utf-8.
 * 通过获取子节点方法
 * @author 朱志杰
 *
 */
public class XMLUtil {
    /*
     * XML 文件文档
     */
    private Document doc=null;
    //字符编码
    private String charEncode="utf-8";

    /**
     * 根据xml内容直接生成xml dom
     * @param xmlContent xml内容
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public XMLUtil(String xmlContent) throws ParserConfigurationException, SAXException, IOException{
        StringReader sr = new StringReader(xmlContent);
        InputSource is = new InputSource(sr);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        //不检查DTD
        db.setEntityResolver( new EntityResolver() {
                                  public InputSource resolveEntity(String publicId, String systemId)
                                          throws SAXException, IOException {
                                      return new InputSource(new StringReader(""));
                                  }
                              }
        );
        //读取文件
        doc=db.parse(is);
    }

    /**
     * 根据xml文件路径生成xml dom
     * @param xmlFile xml文件
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public XMLUtil(File xmlFile) throws ParserConfigurationException, SAXException, IOException{
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        //不检查DTD
        db.setEntityResolver( new EntityResolver() {
                                  public InputSource resolveEntity(String publicId, String systemId)
                                          throws SAXException, IOException {
                                      return new InputSource(new StringReader(""));
                                  }
                              }
        );
        //读取文件
        doc=db.parse(xmlFile);
    }

    /**
     * 根据流生成xml dom
     * @param is 流
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public XMLUtil(InputSource is) throws ParserConfigurationException, SAXException, IOException{
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        //不检查DTD
        db.setEntityResolver( new EntityResolver() {
                                  public InputSource resolveEntity(String publicId, String systemId)
                                          throws SAXException, IOException {
                                      return new InputSource(new StringReader(""));
                                  }
                              }
        );
        //读取文件
        doc=db.parse(is);
    }

    /**
     * 根据节点名称序号读取节点
     * @param nodeName 节点名称
     * @param index 第几个，序号从0开始。
     * @return Node
     */
    public Node readNode(String nodeName,int index){
        NodeList list= doc.getElementsByTagName(nodeName);
        return list.item(index);
    }

    /**
     * 根据节点名称读取节点列表
     * @param nodeName 节点名称
     * @return NodeList
     */
    public NodeList readNodeList(String nodeName){
        return doc.getElementsByTagName(nodeName);
    }

    /**
     * 得到某个节点的值
     * @param node 某个节点
     * @return String
     */
    public String getNodeValue(Node node){
        return node.getNodeValue();
    }

    /**
     * 得到某个元素属性的值
     * @param element 元素
     * @param attr 属性名
     * @return String
     */
    public String getElementAttr(Element element,String attr){
        return element.getAttribute(attr);
    }


    /**
     * 更新某个节点的内容
     * @param node 节点
     * @param val 更新的值
     */
    public void setNodeValue(Node node,String val){
        node.setNodeValue(val);
    }

    /**
     * 更新某个节点的属性
     * @param element 节点
     * @param attr 属性名
     * @param attrVal 属性值
     */
    public void setElementAttr(Element element, String attr,String attrVal){
        element.setAttribute(attr,attrVal);
    }

    /**
     * XML org.w3c.dom.Document 转 String
     * @throws Exception
     */
    public String xmlToString() throws Exception {
        // XML转字符串
        String xmlStr = "";
        TransformerFactory tf = TransformerFactory.newInstance();
        <fim_suffix>
        t.setOutputProperty("encoding", this.charEncode);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        t.transform(new DOMSource(doc), new StreamResult(bos));
        xmlStr = bos.toString();
        return xmlStr;
    }

    /**
     * 设置编码。不设置采用Utf-8编码。
     * @param charEncode 编码 如：utf-8
     */
    public void setCharEncode(String charEncode) {
        this.charEncode = charEncode;
    }

//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		try {
//			XMLUtil a=new XMLUtil("d://aaa.xml");
//			System.out.println(a.xmlToString());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

}

<fim_middle>