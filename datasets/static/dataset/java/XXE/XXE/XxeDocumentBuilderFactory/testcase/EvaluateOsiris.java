<filename>src/main/java/de/hu/berlin/wbi/process/osiris/EvaluateOsiris.java<fim_prefix>

package de.hu.berlin.wbi.process.osiris;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.hu.berlin.wbi.objects.DatabaseConnection;
import de.hu.berlin.wbi.objects.MutationMention;
import de.hu.berlin.wbi.objects.UniprotFeature;
import de.hu.berlin.wbi.objects.dbSNP;
import de.hu.berlin.wbi.objects.dbSNPNormalized;


public class EvaluateOsiris {

    /**
     * Evaluate our normalization procedure on the Osiris corpus.
     *
     * @param args  Property XML and Osiris-corpus as parameters
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws XPathExpressionException
     * @throws TransformerFactoryConfigurationError
     * @throws TransformerException
     * @throws SQLException
     */
    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException, TransformerFactoryConfigurationError, TransformerException,  SQLException {

        String propertyFile = args[0];
        String osirisCorpus = args[1];
        Properties property = new Properties();
        try {
            property.loadFromXML(new FileInputStream(new File(propertyFile)));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        final DatabaseConnection mysql = new DatabaseConnection(property);
        mysql.connect(); //Connect with local mySQL Database
        dbSNP.init(mysql, property.getProperty("database.PSM"), property.getProperty("database.hgvs_view"));
        UniprotFeature.init(mysql, property.getProperty("database.uniprot"));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        <fim_suffix>
        DocumentBuilder builder = factory.newDocumentBuilder();

        XPath xPath = XPathFactory.newInstance().newXPath();
        XPathExpression documentExp = xPath.compile("/Articles/Article");
        XPathExpression pmidExp = xPath.compile("./Pmid");
        XPathExpression geneExp = xPath.compile(".//gene");
        XPathExpression variantExp = xPath.compile(".//variant");

        Document document = builder.parse(osirisCorpus);
        NodeList documents =  (NodeList) documentExp.evaluate(document, XPathConstants.NODESET);	//Corpus

        Pattern p = Pattern.compile("^-?[1-9]+[0-9]*");
        int tp=0; int fp=0; int fn=0;

        //Iterate single documents in OSIRIS corpus
        for (int i =0; i < documents.getLength(); i++) {
            Node doc = documents.item(i);


            //Extract current PubMed identifier
            NodeList pmidNode = (NodeList) pmidExp.evaluate(doc, XPathConstants.NODESET);
            if(pmidNode.getLength() != 1)
                throw new RuntimeException("Found " +pmidNode.getLength() +" PMID nodes");
            int pmid = Integer.parseInt(pmidNode.item(0).getTextContent());

            //Extract all Entrez-Genes in this article
            List<Integer> genes = new ArrayList<Integer>();
            NodeList geneNode = (NodeList) geneExp.evaluate(doc, XPathConstants.NODESET);
            for(int j =0; j < geneNode.getLength(); j++){
                Node gene = geneNode.item(j);

                if(gene.getAttributes().getNamedItem("g_id").getTextContent().equals("No"))
                    continue;

                genes.add(Integer.parseInt(gene.getAttributes().getNamedItem("g_id").getTextContent()));
            }

            //Exctract all normalized mutation mentions in this article (rs-ID to string-mentions)
            //mutationVariation: (e.g. rs1805126 -> [D1819D, C5457T])

            //Comment: I, personally, thing it would be better to use a HashMap<Integer, Set<String>> representation
            //Such a representation would evaluate duplicate String mentions only once (e.g., Ala12Tyr), which is less prone
            //to frequent repeats of the same mention...
            HashMap<Integer, List<String>> mutationVariation = new HashMap<Integer, List<String>>();
            NodeList variantNode = (NodeList) variantExp.evaluate(doc, XPathConstants.NODESET);
            for(int j =0; j < variantNode.getLength(); j++){
                Node variant = variantNode.item(j);

                String rsId = variant.getAttributes().getNamedItem("v_id").getTextContent();
                //Skip mutations which are not normalized to a dbSNP identifier
                if(rsId.equals("No"))
                    continue;

                int correctRsId = Integer.parseInt(rsId);
                String mutationString = variant.getAttributes().getNamedItem("v_norm").getTextContent();

                //Convert allele mentions into synonymous substitutions (e.g., 196R -> R196R)
                Matcher m = p.matcher(mutationString);
                if(m.find()){
                    mutationString = mutationString.charAt(mutationString.length()-1) +mutationString;
                }

                if(mutationVariation.containsKey(correctRsId)){
                    mutationVariation.get(correctRsId).add(mutationString);
                }
                else{
                    List<String> tmpSet = new ArrayList<String>();
                    tmpSet.add(mutationString);
                    mutationVariation.put(correctRsId, tmpSet);
                }
            }

            //Perform normalization with the same strategy as thomas2011
            for(int rs : mutationVariation.keySet()){

                Set<Integer> normalized_rsIDs = new HashSet<Integer>();
                for(String mutationString : mutationVariation.get(rs)){		//For each mutation string:

                    if(mutationString.equals("rs" +rs)){
                        normalized_rsIDs.add(rs);
                    }

                    else{
                        MutationMention mutation = new MutationMention(mutationString); //Build a SNP representation, which we will try to normalize
                        //Perform nornalization  for all genes in the article
                        for(int gene :  genes){
                            final List<dbSNP> potentialSNPs = dbSNP.getSNP(gene);
                            final List<UniprotFeature> features = UniprotFeature.getFeatures(gene);
                            mutation.normalizeSNP(potentialSNPs, features, false);

                            //And here we have  a list of all dbSNPs with which I could successfully associate the mutation
                            final List<dbSNPNormalized> normalized = mutation.getNormalized();
                            for(dbSNPNormalized norm : normalized)
                                normalized_rsIDs.add(norm.getRsID());
                        }
                    }

                    //Evaluation
                    if(normalized_rsIDs.contains(rs)){		//Check if found rsID's  is correct
                        tp++;
                        normalized_rsIDs.remove(rs);
                    }
                    else{					//Otherwise we have a false negative
                        fn++;
                        System.out.println("False negative in: " +pmid  +" " +mutationVariation.get(rs).toString() +" gene=" +genes.toString() +" rs" +rs);
                    }

                    fp+=normalized_rsIDs.size();			//All remaining ids are false positives
                }
            }
        }

        double recall = (double) tp/(tp+fn);
        double precision = (double) tp/(tp+fp);
        double f1 = 2*(precision*recall)/(precision+recall);

        DecimalFormat df = new DecimalFormat( "0.00" );
        System.err.println("TP " +tp);
        System.err.println("FP " +fp);
        System.err.println("FN " +fn);
        System.err.println("Precision " +df.format(precision));
        System.err.println("Recall " +df.format(recall));
        System.err.println("F1 " +df.format(f1));
    }

}
<fim_middle>