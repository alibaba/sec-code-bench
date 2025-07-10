<filename>NightgamesMod/nightgames/characters/TraitTree.java<fim_prefix>

package nightgames.characters;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class TraitTree {
    private interface TraitRequirement {
        boolean meetsRequirement(Character self);
    }

    public class RequirementXmlHandler extends DefaultHandler {
        private HashMap<Trait, List<TraitRequirement>> requirements;
        private String text;
        private String trait;
        private List<TraitRequirement> reqs;
        private Attribute att;

        public HashMap<Trait, List<TraitRequirement>> getRequirements() {
            return requirements;
        }

        @Override
        public void startDocument() throws SAXException {
            super.startDocument();
            requirements = new HashMap<>();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            if (qName.equals("Requirements")) {
                reqs = new ArrayList<>();
            } else if (qName.equals("AttributeReq")) {
                att = Attribute.valueOf(attributes.getValue("type").trim());
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            try {
                super.characters(ch, start, length);
            } catch (SAXException e) {
                e.printStackTrace();
            }
            text = new String(Arrays.copyOfRange(ch, start, start + length));
        }

        /**
         * where the real stuff happens
         */
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            final String val = text;
            if (qName.equals("Name")) {
                trait = text;
            } else if (qName.equals("Trait")) {
                requirements.put(Trait.valueOf(trait), reqs);
            } else if (qName.equals("LevelReq")) {
                reqs.add(c -> c.getLevel() > Integer.valueOf(val.trim()));
            } else if (qName.equals("TraitReq")) {
                reqs.add(c -> c.has(Trait.valueOf(val.trim())));
            } else if (qName.equals("NoTraitReq")) {
                reqs.add(c -> !c.has(Trait.valueOf(val.trim())));
            } else if (qName.equals("BreastsReq")) {
                reqs.add(c -> c.body.getLargestBreasts().getSize() >= Integer.valueOf(val.trim()));
            } else if (qName.equals("AttributeReq")) {
                final Attribute attribute = att;
                reqs.add(c -> c.getPure(attribute) > Integer.valueOf(val.trim()));
            } else if (qName.equals("BodypartReq")) {
                reqs.add(c -> c.body.has(val.trim()));
            }
        }
    }

    private HashMap<Trait, List<TraitRequirement>> requirements;

    public TraitTree(InputStream xml) {
        try {
            RequirementXmlHandler handler = new RequirementXmlHandler();
            XMLReader parser = XMLReaderFactory.createXMLReader();
            <fim_suffix>
            parser.parse(new InputSource(xml));
            requirements = handler.getRequirements();
        } catch (SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    public boolean meetsRequirements(Character c, Trait t) {
        return requirements.get(t).parallelStream().allMatch(req -> req.meetsRequirement(c));
    }

    public List<Trait> availTraits(Character c) {
        return requirements.keySet().stream().filter(key -> meetsRequirements(c, key)).collect(Collectors.toList());
    }
}

<fim_middle>