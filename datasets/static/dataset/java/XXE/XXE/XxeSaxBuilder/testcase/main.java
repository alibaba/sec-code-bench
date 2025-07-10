<filename>main.java<fim_prefix>

package commander;

import java.io.File;
import java.io.FileInputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;



public class main {

    /**
     * @param args
     * @throws IOException
     * @throws JDOMException
     */
    //static void sendfirstrun(){



    //}


    public static void main(String[] args) throws JDOMException, IOException {
        //String address="/home/yu/repeatbugreport";
        String address=".";


        ArrayList<ArrayList<Integer>> abovecommands=new ArrayList<>();
        System.out.println("main");


        ////////////////////read the nlp and allcases/////////////
        SAXBuilder sbnlp = new SAXBuilder();//generate the builder
        <fim_suffix>
        String path=address+"/middleResults/nlp.xml";
        Document docnlp=sbnlp.build(new FileInputStream(path));//read the file
        Element rootnlp=docnlp.getRootElement();


        ArrayList<Step> nlpsteplist=new ArrayList<Step>();//record step by step


        String rotateOrNot="false";
        boolean backOrNot=false;

        List<Element> steplist= rootnlp.getChildren();

        ArrayList<String> typeWhatList=new ArrayList<String>();// the no typewhere item is at the front of the list and the have one is at the behind of the list.
        ArrayList<String> digitTypeWhatList=new ArrayList<String>();



        HashMap<String,String> specificSymbolTrans=new HashMap<String,String>();
        ///add another specificSymbol dict
        specificSymbolTrans.put("apostrophe","'");
        specificSymbolTrans.put("comma",",");
        specificSymbolTrans.put("colon",":");
        specificSymbolTrans.put("semicolon",";");
        specificSymbolTrans.put("hyphen","-");
        specificSymbolTrans.put("parentheses","()");
        specificSymbolTrans.put("quote","\"");
        specificSymbolTrans.put("space","realspace");


        for(Element stepelement: steplist){///nlp part
            Step step=new Step();
            List<Element> sentelelist=stepelement.getChild("sentence").getChildren();
            for(Element sentele : sentelelist){
                String word=sentele.getText();
                step.getSentence().add(word);
                ///System.out.println(word);
            }

            String sentenceid=stepelement.getChild("sentenceid").getValue();
            step.setSentenceid(Integer.valueOf(sentenceid));
            ///System.out.println(step.getSentenceid());

            String type=stepelement.getChild("type").getText();
            ///click case
            if(type.equals("click")){
                step.setType(type);


                ///clickwhere case
                for(Element clickwhereele: stepelement.getChild("clickwhere").getChildren()){
                    ArrayList<String> itemlist=new ArrayList<String>();
                    step.getClickwhere().add(itemlist);

                    for(Element itemele: clickwhereele.getChildren()){
                        itemlist.add(itemele.getText().replaceAll("[^a-zA-Z ^0-9]", ""));
                    }
                }

                if(stepelement.getChild("clicktimes").getText().startsWith("False")){
                    step.setClicktimes(false);
                }else{
                    step.setClicktimes(true);
                }

                if(stepelement.getChild("clicktimes").getText().equals("long")){
                    step.setClicktype("long");
                }else{
                    step.setClicktype("short");
                }
                nlpsteplist.add(step);
            }

            ///input case
            if(type.equals("input")){
                step.setType(type);

                if(stepelement.getChild("inputtimes").getText().startsWith("False")){
                    step.setTypetimes(false);
                }else{
                    step.setTypetimes(true);
                }

                ///inputwhere case
                for(Element clickwhereele: stepelement.getChild("typewhere").getChildren()){
                    ArrayList<String> itemlist=new ArrayList<String>();
                    step.getTypewhere().add(itemlist);

                    for(Element itemele: clickwhereele.getChildren()){
                        itemlist.add(itemele.getText().replaceAll("[^a-zA-Z ^0-9]", ""));
                    }
                }

                ///inputwhat case
                int onlyfirst=1;//the nlp will give more than one result, at here the typeWhatList only add the first one.
                for(Element clickwhereele: stepelement.getChild("typewhat").getChildren()){
                    ArrayList<String> itemlist=new ArrayList<String>();
                    step.getTypewhat().add(itemlist);

                    for(Element itemele: clickwhereele.getChildren()){
                        String itemText=itemele.getText();

						/*
						for(String SpecialSymbolic:specificSymbolTrans.keySet()){
							if(itemText.contains(SpecialSymbolic)){
								itemText=itemText.replaceAll(SpecialSymbolic, specificSymbolTrans.get(SpecialSymbolic));
								break;//consider only one much.
							}
						}
						*/
                        itemlist.add(itemText);

                        if(onlyfirst==1){
                            if(stepelement.getChild("typewhere").getChildren().isEmpty() && stepelement.getChild("typewhere").getChildren().isEmpty()){
                                typeWhatList.add(0,itemText);
                            }else{
                                typeWhatList.add(itemText);
                            }
                            onlyfirst=0;
                        }
                        //itemlist.add(itemele.getText());
                    }
                }



                ///inputdigitwhere case
                for(Element clickwhereele: stepelement.getChild("digittypewhere").getChildren()){
                    ArrayList<String> itemlist=new ArrayList<String>();
                    step.getDigittypewhere().add(itemlist);

                    for(Element itemele: clickwhereele.getChildren()){
                        itemlist.add(itemele.getText().replaceAll("[^a-zA-Z ^0-9]", ""));
                    }
                }

                ///inputdigitwhat case
                int digitonlyfirst=1;
                for(Element clickwhereele: stepelement.getChild("digittypewhat").getChildren()){
                    ArrayList<String> itemlist=new ArrayList<String>();
                    step.getDigittypewhat().add(itemlist);

                    for(Element itemele: clickwhereele.getChildren()){
                        String itemText=itemele.getText();


                        itemlist.add(itemText);

                        if(digitonlyfirst==1){
                            if(stepelement.getChild("digittypewhere").getChildren().isEmpty() && stepelement.getChild("digittypewhere").getChildren().isEmpty()){
                                digitTypeWhatList.add(0,itemText);
                            }else{
                                digitTypeWhatList.add(itemText);
                            }
                            onlyfirst=0;
                        }
                        //itemlist.add(itemele.getText());
                    }
                }
                nlpsteplist.add(step);
            }

            ///rotate case



			/*
			if(type.equals("rotate")){
				rotateOrNot=true;

				//step.setType(type);
				//nlpsteplist.add(step);

			}
			*/

            ///create case
            if(type.equals("create")){
                step.setType(type);

                ///createwhat case
                for(Element clickwhereele: stepelement.getChild("createwhat").getChildren()){
                    ArrayList<String> itemlist=new ArrayList<String>();
                    step.getCreatewhat().add(itemlist);

                    for(Element itemele: clickwhereele.getChildren()){
                        itemlist.add(itemele.getText().replaceAll("[^a-zA-Z ^0-9]", ""));
                    }
                }
                nlpsteplist.add(step);
            }

            ///cancel case
            if(type.equals("cancel")){
                step.setType(type);
                nlpsteplist.add(step);
            }


        }



    }//end main

}


<fim_middle>