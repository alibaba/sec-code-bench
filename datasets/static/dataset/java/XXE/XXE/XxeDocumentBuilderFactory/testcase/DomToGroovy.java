<filename>subprojects/groovy-xml/src/main/java/org/codehaus/groovy/tools/xml/DomToGroovy.java<fim_prefix>

/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.codehaus.groovy.tools.xml;

import groovy.util.IndentPrinter;
import org.w3c.dom.*;
import org.codehaus.groovy.syntax.Types;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.InputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

/**
 * A SAX handler for turning XML into Groovy scripts
 *
 * @author James Strachan
 * @author paulk
 */
public class DomToGroovy {

    protected IndentPrinter out;
    protected boolean inMixed = false;
    protected String qt = "'";
    protected Collection<String> keywords = Types.getKeywords();

    public DomToGroovy(PrintWriter out) {
        this(new IndentPrinter(out));
    }

    // TODO allow string quoting delimiter to be specified, e.g. ' vs "
    public DomToGroovy(IndentPrinter out) {
        this.out = out;
    }

    public void print(Document document) {
        printChildren(document, new HashMap());
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: DomToGroovy infilename [outfilename]");
            System.exit(1);
        }
        Document document = null;
        try {
            document = parse(args[0]);
        } catch (Exception e) {
            System.out.println("Unable to parse input file '" + args[0] + "': " + e.getMessage());
            System.exit(1);
        }
        PrintWriter writer = null;
        if (args.length < 2) {
            writer = new PrintWriter(System.out);
        } else {
            try {
                writer = new PrintWriter(new FileWriter(new File(args[1])));
            } catch (IOException e) {
                System.out.println("Unable to create output file '" + args[1] + "': " + e.getMessage());
                System.exit(1);
            }
        }
        DomToGroovy converter = new DomToGroovy(writer);
        converter.out.incrementIndent();
        writer.println("#!/bin/groovy");
        writer.println();
        writer.println("// generated from " + args[0]);
        writer.println("System.out << new groovy.xml.StreamingMarkupBuilder().bind {");
        converter.print(document);
        writer.println("}");
        writer.close();
    }

    // Implementation methods
    //-------------------------------------------------------------------------

    protected static Document parse(final String fileName) throws Exception {
        return parse(new File(fileName));
    }

    public static Document parse(final File file) throws Exception {
        return parse(new BufferedReader(new FileReader(file)));
    }

    public static Document parse(final Reader input) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(input));
    }

    public static Document parse(final InputStream input) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        <fim_suffix>
        return builder.parse(new InputSource(input));
    }

    protected void print(Node node, Map namespaces, boolean endWithComma) {
        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE :
                printElement((Element) node, namespaces, endWithComma);
                break;
            case Node.PROCESSING_INSTRUCTION_NODE :
                printPI((ProcessingInstruction) node, endWithComma);
                break;
            case Node.TEXT_NODE :
                printText((Text) node, endWithComma);
                break;
            case Node.COMMENT_NODE :
                printComment((Comment) node, endWithComma);
                break;
        }
    }

    protected void printElement(Element element, Map namespaces, boolean endWithComma) {
        namespaces = defineNamespaces(element, namespaces);

        element.normalize();
        printIndent();

        String prefix = element.getPrefix();
        boolean hasPrefix = prefix != null && prefix.length() > 0;
        String localName = getLocalName(element);
        boolean isKeyword = checkEscaping(localName);
        if (isKeyword || hasPrefix) print(qt);
        if (hasPrefix) {
            print(prefix);
            print(".");
        }
        print(localName);
        if (isKeyword || hasPrefix) print(qt);
        print("(");

        boolean hasAttributes = printAttributes(element);

        NodeList list = element.getChildNodes();
        int length = list.getLength();
        if (length == 0) {
            printEnd(")", endWithComma);
        } else {
            Node node = list.item(0);
            if (length == 1 && node instanceof Text) {
                Text textNode = (Text) node;
                String text = getTextNodeData(textNode);
                if (hasAttributes) print(", ");
                printQuoted(text);
                printEnd(")", endWithComma);
            } else if (mixedContent(list)) {
                println(") {");
                out.incrementIndent();
                boolean oldInMixed = inMixed;
                inMixed = true;
                for (node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
                    print(node, namespaces, false);
                }
                inMixed = oldInMixed;
                out.decrementIndent();
                printIndent();
                printEnd("}", endWithComma);
            } else {
                println(") {");
                out.incrementIndent();
                printChildren(element, namespaces);
                out.decrementIndent();
                printIndent();
                printEnd("}", endWithComma);
            }
        }
    }

    protected void printQuoted(String text) {
        if (text.indexOf("\n") != -1) {
            print("'''");
            print(text);
            print("'''");
        } else {
            print(qt);
            print(escapeQuote(text));
            print(qt);
        }
    }

    protected void printPI(ProcessingInstruction instruction, boolean endWithComma) {
        printIndent();
        print("mkp.pi(" + qt);
        print(instruction.getTarget());
        print(qt + ", " + qt);
        print(instruction.getData());
        printEnd(qt + ");", endWithComma);
    }

    protected void printComment(Comment comment, boolean endWithComma) {
        String text = comment.getData().trim();
        if (text.length() >0) {
            printIndent();
            print("/* ");
            print(text);
            printEnd(" */", endWithComma);
        }
    }

    protected void printText(Text node, boolean endWithComma) {
        String text = getTextNodeData(node);
        if (text.length() > 0) {
            printIndent();
            if (inMixed) print("mkp.yield ");
            printQuoted(text);
            printEnd("", endWithComma);
        }
    }

    protected String escapeQuote(String text) {
        return text.replaceAll("\\\\", "\\\\\\\\").replaceAll(qt, "\\\\" + qt);
    }

    protected Map defineNamespaces(Element element, Map namespaces) {
        Map answer = null;
        String prefix = element.getPrefix();
        if (prefix != null && prefix.length() > 0 && !namespaces.containsKey(prefix)) {
            answer = new HashMap(namespaces);
            defineNamespace(answer, prefix, element.getNamespaceURI());
        }
        NamedNodeMap attributes = element.getAttributes();
        int length = attributes.getLength();
        for (int i = 0; i < length; i++) {
            Attr attribute = (Attr) attributes.item(i);
            prefix = attribute.getPrefix();
            if (prefix != null && prefix.length() > 0 && !namespaces.containsKey(prefix)) {
                if (answer == null) {
                    answer = new HashMap(namespaces);
                }
                defineNamespace(answer, prefix, attribute.getNamespaceURI());
            }
        }
        return (answer != null) ? answer : namespaces;
    }

    protected void defineNamespace(Map namespaces, String prefix, String uri) {
        namespaces.put(prefix, uri);
        if (!prefix.equals("xmlns") && !prefix.equals("xml")) {
            printIndent();
            print("mkp.declareNamespace(");
            print(prefix);
            print(":" + qt);
            print(uri);
            println(qt + ")");
        }
    }
}
<fim_middle>