<filename>src/husacct/control/task/resources/HusacctResource.java<fim_prefix>

package husacct.control.task.resources;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class HusacctResource implements IResource{

    private Logger logger = Logger.getLogger(HusacctResource.class);

    // TODO: Decrypt file
    @Override
    public Document load(HashMap<String, Object> dataValues) {
        File file = (File) dataValues.get("file");
        SAXBuilder sax = new SAXBuilder();
        <fim_suffix>
        try {
            doc = sax.build(file);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return doc;
    }

    // TODO: Encrypt file
    @Override
    public boolean save(Document doc, HashMap<String, Object> dataValues) {

        File file = (File) dataValues.get("file");
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            xout.output(doc, outputStream);
            outputStream.close();
            return true;
        } catch (Exception e){
            logger.error(e.getMessage());
            new RuntimeException(e);
        }
        return false;
    }

    @Override
    public boolean save(Document doc, HashMap<String, Object> dataValues,
                        HashMap<String, Object> config) {
        // TODO Auto-generated method stub
        return false;
    }

}
<fim_middle>