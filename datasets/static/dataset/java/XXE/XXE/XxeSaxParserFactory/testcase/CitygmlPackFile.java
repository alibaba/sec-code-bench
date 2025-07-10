<filename>src/main/java/osm/surveyor/tools/CitygmlPackFile.java<fim_prefix>

package osm.surveyor.tools;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import lombok.Getter;

@SuppressWarnings("serial")
@Getter
public class CitygmlPackFile extends File {
    CitygmlSurveyYearParser gml;

    public CitygmlPackFile(File file) throws ParserConfigurationException, SAXException, IOException, ParseException {
        super(file.getParentFile(), file.getName());
        this.gml = new CitygmlSurveyYearParser();
    }

    /**
     * XMLパースを実行する
     *
     */
    public void parse() throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        <fim_suffix>
        factory.setValidating(false);

        SAXParser parser = factory.newSAXParser();
        try {
            parser.parse(this, gml);
        } catch (SAXParseException e) {}
    }
}

<fim_middle>