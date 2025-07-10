<filename>src/main/java/com/pega/gcs/logviewer/systemstate/model/PRConfig.java<fim_prefix>

package com.pega.gcs.logviewer.systemstate.model;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pega.gcs.fringecommon.utilities.KeyValuePair;

public class PRConfig implements Comparable<PRConfig> {

    @JsonProperty("FileName")
    private String fileName;

    @JsonProperty("FilePath")
    private String filePath;

    @JsonProperty("XMLContent")
    private String xmlContent;

    @JsonProperty("PZ_ERROR")
    private String pzError;

    private List<KeyValuePair<String, String>> settingList;

    public PRConfig() {
        super();
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getXmlContent() {
        return xmlContent;
    }

    public String getPzError() {
        return pzError;
    }

    public List<KeyValuePair<String, String>> getSettingList() {
        return settingList;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, filePath, xmlContent);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PRConfig)) {
            return false;
        }
        PRConfig other = (PRConfig) obj;
        return Objects.equals(fileName, other.fileName) && Objects.equals(filePath, other.filePath)
                && Objects.equals(xmlContent, other.xmlContent);
    }

    @Override
    public String toString() {
        return "PRConfig [fileName=" + fileName + ", filePath=" + filePath + ", xmlContent=" + xmlContent + "]";
    }

    @Override
    public int compareTo(PRConfig other) {

        int compare = this.fileName.compareTo(other.fileName);

        if (compare == 0) {
            compare = this.filePath.compareTo(other.filePath);
        }

        if (compare == 0) {
            compare = this.xmlContent.compareTo(other.xmlContent);
        }

        return compare;
    }

    public void postProcess() throws DocumentException {

        settingList = new ArrayList<>();

        if (xmlContent != null) {

            SAXReader saxReader = new SAXReader();
            <fim_suffix>
            Document doc = saxReader.read(stringReader);

            Element pegarulesElement = doc.getRootElement();

            @SuppressWarnings("unchecked")
            Iterator<Element> elemIt = pegarulesElement.elementIterator();

            while (elemIt.hasNext()) {

                Element envElem = elemIt.next();

                String settingName = envElem.attributeValue("name");
                String settingValue = envElem.attributeValue("value");

                KeyValuePair<String, String> settingPair = new KeyValuePair<>(settingName, settingValue);

                settingList.add(settingPair);
            }
        }

    }
}

<fim_middle>