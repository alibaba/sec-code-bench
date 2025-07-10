<filename>curso-spring/curso-spring-security/capa-web/src/main/java/com/indizen/cursoSpring/web/gui/importer/ParseImporterXML.java<fim_prefix>

package com.indizen.cursoSpring.web.gui.importer;
import java.io.File;
import java.io.IOException;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

import com.indizen.cursoSpring.web.util.Constants;

public class ParseImporterXML {

    public ParseImporterXML() {
        super();
    }

    public Importer parseXML(String xml, String dir) throws IOException,SAXException {

        Digester digester = new Digester();
        <fim_suffix>

        digester.addObjectCreate(Constants.FIELD, Field.class);
        digester.addSetProperties(Constants.FIELD, "id", "id");
        digester.addSetProperties(Constants.FIELD, "compulsory","compulsory");
        digester.addSetNext(Constants.FIELD, "setFields");

        digester.addObjectCreate(Constants.VALIDATION,ValidationRule.class);
        digester.addSetProperties(Constants.VALIDATION,"regex", "regex");
        digester.addSetProperties(Constants.VALIDATION,"errormsg", "errormsg");
        digester.addSetNext(Constants.VALIDATION,"setValidationRules");

        File importerFile = new File(xml);
        return (Importer) digester.parse(importerFile);
    }
}
<fim_middle>