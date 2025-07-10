<filename>web/src/main/java/org/akaza/openclinica/controller/openrosa/processor/ItemProcessor.java<fim_prefix>

package org.akaza.openclinica.controller.openrosa.processor;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.akaza.openclinica.controller.openrosa.ItemItemDataContainer;
import org.akaza.openclinica.controller.openrosa.PformValidator;
import org.akaza.openclinica.controller.openrosa.SubmissionContainer;
import org.akaza.openclinica.dao.hibernate.CrfVersionDao;
import org.akaza.openclinica.dao.hibernate.DiscrepancyNoteDao;
import org.akaza.openclinica.dao.hibernate.DiscrepancyNoteTypeDao;
import org.akaza.openclinica.dao.hibernate.DnItemDataMapDao;
import org.akaza.openclinica.dao.hibernate.ItemDao;
import org.akaza.openclinica.dao.hibernate.ItemDataDao;
import org.akaza.openclinica.dao.hibernate.ItemFormMetadataDao;
import org.akaza.openclinica.dao.hibernate.ItemGroupDao;
import org.akaza.openclinica.dao.hibernate.ItemGroupMetadataDao;
import org.akaza.openclinica.dao.hibernate.ResolutionStatusDao;
import org.akaza.openclinica.domain.Status;
import org.akaza.openclinica.domain.datamap.CrfVersion;
import org.akaza.openclinica.domain.datamap.DiscrepancyNote;
import org.akaza.openclinica.domain.datamap.DnItemDataMap;
import org.akaza.openclinica.domain.datamap.DnItemDataMapId;
import org.akaza.openclinica.domain.datamap.EventCrf;
import org.akaza.openclinica.domain.datamap.Item;
import org.akaza.openclinica.domain.datamap.ItemData;
import org.akaza.openclinica.domain.datamap.ItemFormMetadata;
import org.akaza.openclinica.domain.datamap.ItemGroup;
import org.akaza.openclinica.domain.datamap.ItemGroupMetadata;
import org.akaza.openclinica.domain.datamap.ResolutionStatus;
import org.akaza.openclinica.domain.datamap.Study;
import org.akaza.openclinica.domain.datamap.StudySubject;
import org.akaza.openclinica.domain.user.UserAccount;
import org.akaza.openclinica.i18n.util.ResourceBundleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Errors;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Component
public class ItemProcessor implements Processor, Ordered {

    @Autowired
    ItemDataDao itemDataDao;

    @Autowired
    ItemDao itemDao;

    @Autowired
    ItemGroupDao itemGroupDao;

    @Autowired
    ItemGroupMetadataDao itemGroupMetadataDao;

    @Autowired
    ItemFormMetadataDao itemFormMetadataDao;

    @Autowired
    CrfVersionDao crfVersionDao;

    @Autowired
    DiscrepancyNoteDao discrepancyNoteDao;

    @Autowired
    ResolutionStatusDao resolutionStatusDao;

    @Autowired
    DiscrepancyNoteTypeDao discrepancyNoteTypeDao;

    @Autowired
    DnItemDataMapDao dnItemDataMapDao;

    protected final Logger logger = LoggerFactory.getLogger(getClass().getName());

    public int getOrder() {
        return 4;
    }

    public void process(SubmissionContainer container) throws Exception {
        logger.info("Executing Item Processor.");
        ArrayList<HashMap> listOfUploadFilePaths =container.getListOfUploadFilePaths();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        <fim_suffix>
        DocumentBuilder db = dbf.newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(container.getRequestBody()));
        Document doc = db.parse(is);
        String itemName;
        String itemValue;
        String groupNodeName = "";

        NodeList instanceNodeList = doc.getElementsByTagName("instance");
        // Instance loop
        for (int i = 0; i < instanceNodeList.getLength(); i = i + 1) {
            Node instanceNode = instanceNodeList.item(i);
            if (instanceNode instanceof Element) {
                NodeList crfNodeList = instanceNode.getChildNodes();
                // Form loop
                for (int j = 0; j < crfNodeList.getLength(); j = j + 1) {
                    Node crfNode = crfNodeList.item(j);
                    if (crfNode instanceof Element) {
                        CrfVersion crfVersion = container.getEventCrf().getCrfVersion();
                        EventCrf eventCrf = container.getEventCrf();
                        ArrayList<ItemData> itemDataList = new ArrayList<ItemData>();

                        HashMap<Integer,Set<Integer>> groupOrdinalMapping = new HashMap<Integer,Set<Integer>>();
                        NodeList groupNodeList = crfNode.getChildNodes();

                        List<Item> items = itemDao.findAllByCrfVersionId(crfVersion.getCrfVersionId());
                        List<ItemData> itemDatas = itemDataDao.findAllByEventCrf(container.getEventCrf().getEventCrfId());
                        List<ItemGroup> itemGroups = itemGroupDao.findByCrfVersionId(crfVersion.getCrfVersionId());
                        List<ItemGroupMetadata> itemGroupMetadatas = itemGroupMetadataDao.findAllByCrfVersion(crfVersion.getCrfVersionId());
                        List<ItemFormMetadata> itemFormMetadatas = itemFormMetadataDao.findAllByCrfVersion(crfVersion.getCrfVersionId());

                        // Group loop
                        for (int k = 0; k < groupNodeList.getLength(); k = k + 1) {
                            Node groupNode = groupNodeList.item(k);
                            if (groupNode instanceof Element && !groupNode.getNodeName().startsWith("SECTION_")) {
                                groupNodeName = groupNode.getNodeName();
                                ItemGroup itemGroup = lookupItemGroup(groupNodeName, crfVersion, itemGroups);
                                if (itemGroup == null) {
                                    logger.error("Failed to lookup item group: '" + groupNodeName + "'.  Continuing with submission.");
                                    continue;
                                }

                                if (itemGroup != null && !groupOrdinalMapping.containsKey(itemGroup.getItemGroupId())) groupOrdinalMapping.put(itemGroup.getItemGroupId(),new TreeSet<Integer>());

                                NodeList itemNodeList = groupNode.getChildNodes();
                                // Item loop
                                for (int m = 0; m < itemNodeList.getLength(); m = m + 1) {
                                    Node itemNode = itemNodeList.item(m);
                                    if (itemNode instanceof Element && !itemNode.getNodeName().endsWith(".HEADER")
                                            && !itemNode.getNodeName().endsWith(".SUBHEADER")
                                            && !itemNode.getNodeName().equals("OC.REPEAT_ORDINAL")
                                            && !itemNode.getNodeName().equals("OC.STUDY_SUBJECT_ID")
                                            && !itemNode.getNodeName().equals("OC.STUDY_SUBJECT_ID_CONFIRM") ) {

                                        itemName = itemNode.getNodeName().trim();
                                        itemValue = itemNode.getTextContent();

                                        Item item = lookupItem(itemName, crfVersion, items);
                                        if (item == null) {
                                            logger.error("Failed to lookup item: '" + itemName + "'.  Continuing with submission.");
                                            continue;
                                        }


                                    }
                                }
                            }
                        }
                        // Delete rows that have been removed
                    }
                }
            }
        }
    }


}
<fim_middle>