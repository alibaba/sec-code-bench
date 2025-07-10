<filename>Semester3/Advanced Programming Methods/Labs/lab3/src/repository/HomeworkXMLRepository.java<fim_prefix>

package repository;

import domain.entities.Homework;
import domain.entities.Student;
import domain.validators.IllegalArgumentException;
import domain.validators.Validator;
import domain.validators.ValidatorException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;

public class HomeworkXMLRepository extends AbstractRepository<Homework, Integer> {
    private String file;
    private DocumentBuilderFactory factory;

    /**
     * XML File Repository Homework Constructor
     * @param v - Student Validator
     * @param fileName - path to the XML File
     */
    public HomeworkXMLRepository(Validator<Homework> v, String fileName){
        super(v);
        this.file = fileName;
        this.factory = DocumentBuilderFactory.newInstance();
        try {
            readFromXML();
        } catch (ValidatorException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }


    /**
     * Function to read Homework from XML File
     * @throws ValidatorException if the homework is't well formed
     * @throws IllegalArgumentException - if parameters are null
     */
    private void readFromXML() throws ValidatorException, IllegalArgumentException {
        try {
            DocumentBuilder db = this.factory.newDocumentBuilder();
            Document doc = db.parse(new File(this.file));
            Element el = (Element) doc.getDocumentElement();

            NodeList nodeList = el.getElementsByTagName("homework");

            for(int i = 0; i<nodeList.getLength(); i++){
                Integer id, recvWeek, deadlineWeek;
                String description;
                Element item = (Element) nodeList.item(i);

                id = Integer.parseInt(item.getAttribute("id"));

                NodeList nodeList1 = item.getElementsByTagName("desc");
                description = nodeList1.item(0).getChildNodes().item(0).getNodeValue();

                nodeList1 = item.getElementsByTagName("recvWeek");

                recvWeek = Integer.parseInt(nodeList1.item(0).getChildNodes().item(0).getNodeValue());

                nodeList1 = item.getElementsByTagName("deadlineWeek");

                deadlineWeek = Integer.parseInt(nodeList1.item(0).getChildNodes().item(0).getNodeValue());

                Homework hw = new Homework(id, description, recvWeek, deadlineWeek);
                super.save(hw);
            }

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Function to write in XML File
     */
    private void writeToXML(){
        try {
            DocumentBuilder db = this.factory.newDocumentBuilder();

            Document doc = db.newDocument();
            Element el = doc.createElement("allHomework");

            doc.appendChild(el);

            for (Homework hw:
                    super.findAll()) {
                el.appendChild(getHomework(doc, hw.getID(), hw.getDescription(), hw.getRecvWeek(), hw.getDeadlineWeek()));
            }

            TransformerFactory tf = TransformerFactory.newInstance();
            <fim_suffix>
            DOMSource ds = new DOMSource(doc);
            StreamResult sRes = new StreamResult(new File(this.file));
            Transformer transformer = tf.newTransformer();
            transformer.transform(ds, sRes);

        } catch (ParserConfigurationException | TransformerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Function to get a new node to add into XML file
     */
    private static Node getHomework(Document doc, Integer id, String description, Integer recvWeek, Integer deadlineWeek){
        Element homework = doc.createElement("homework");

        homework.setAttribute("id", id.toString());

        //create description element
        homework.appendChild(getHomeworkElements(doc, "desc", description));

        //create recvWeek element
        homework.appendChild(getHomeworkElements(doc, "recvWeek", recvWeek.toString()));

        //create deadlineWeek element
        homework.appendChild(getHomeworkElements(doc, "deadlineWeek", deadlineWeek.toString()));

        return homework;
    }

    /**
     * Utility method to create text node
     * @param doc - document type
     * @param name - property name
     * @param value - property value
     * @return a new node
     */
    private static Node getHomeworkElements(Document doc, String name, String value){
        Element node = doc.createElement(name);
        node.appendChild(doc.createTextNode(value));
        return node;
    }

    /**
     * Overriding save function for XML files
     * @param hw - homework to save
     * @return - null if the homework is saved, the student otherwise
     * @throws ValidatorException - if the homework is invalid
     * @throws IllegalArgumentException - if the homework is null
     */
    @Override
    public Homework save(Homework hw) throws ValidatorException, IllegalArgumentException {
        Homework saved = super.save(hw);

        if(saved == null){
            writeToXML();
            return null;
        }
        return saved;
    }

    /**
     * Overiding update function for XML files
     * @param hw - student to update
     * @return - null if the student was updated, the homework otherwise
     * @throws ValidatorException - if the given homework is invalid
     * @throws IllegalArgumentException - if some parameter is null
     */
    @Override
    public Homework update(Homework hw) throws ValidatorException, IllegalArgumentException {
        Homework aux = super.update(hw);
        writeToXML();
        return aux;
    }


    /**
     * Overriding delete method for XML files
     * @param id - id of the homework tu be deleted
     * id must be not null
     * @return - the deleted homework if it was deleted, null otherwise
     * @throws IllegalArgumentException - if id is null
     */
    @Override
    public Homework delete(Integer id) throws IllegalArgumentException {
        Homework aux = super.delete(id);
        writeToXML();
        return aux;
    }
}

<fim_middle>