<filename>use_little_demo-master/javacode/src/main/java/com/aohuan/dodo/javacode/jsoup/sample1/detail/DoSth2.java<fim_prefix>

package com.aohuan.dodo.javacode.jsoup.sample1.detail;

import com.aohuan.dodo.javacode.jsoup.sample1.utils.JianShuUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by dodo_lihao on 2016/11/25.
 * qq: 2390183798
 * <p/>
 * 爬取   简书 自己关注的好友
 * 熟悉 jsoup的正则
 */
public class DoSth2 {


    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        //简单爬取自己粉丝  整个body
//        printDocumentFromURL(JianShuUtils.getFollowerUrl(JianShuUtils.KEY_DODO_ID));

        //简单爬取自己粉丝  具体一个范围内的
//        printSpical(JianShuUtils.getFollowerUrl(JianShuUtils.KEY_DODO_ID) , "ul.users");  //整个符合ul.users，只有一条
//        printSpical(JianShuUtils.getFollowerUrl(JianShuUtils.KEY_DODO_ID) , "div.follow");  //对应div.follow  没有框住数据
//        printSpical(JianShuUtils.getFollowerUrl(JianShuUtils.KEY_DODO_ID) , "h4 a");  //获取h4 a 匹配的数据

//        printSpicalProperty(JianShuUtils.getFollowerUrl(JianShuUtils.KEY_DODO_ID) , "h4 a");  //获取h4 a 匹配的数据， 并且获取要的属性


        printMap(getSpicalProperty(JianShuUtils.getFollowerUrl(JianShuUtils.KEY_DODO_ID), "h4 a"));

    }

    /**
     * 根据url，获得对应的body
     *
     * @param url
     * @throws IOException
     */
    private static void printDocumentFromURL(String url) throws IOException {

<fim_suffix>

//        String content = doc2.toString();
        String content = doc2.body().toString();
        System.out.println(content);
    }


    /**
     * 爬取特别的item
     *
     * @param url
     * @param divClass 例如：div.ClassName    ul.ClassName
     * @throws IOException
     */
    private static void printSpical(String url, String divClass) throws IOException {
        Document doc2 = Jsoup.connect(url).get();
//        String content = doc2.toString();
        String content = doc2.body().toString();
        Elements eles = doc2.select(divClass);
        for (Element ele : eles) {
//            ele.select()
            System.out.println("----");
            System.out.println(ele.toString());
        }
    }


    static int num = 0;

    private static void printSpicalProperty(String url, String divClass) throws IOException {
        Document doc2 = Jsoup.connect(url).get();
        Elements eles = doc2.select(divClass);
        for (Element ele : eles) {
//            ele.select()
            num++;
            System.out.println("----");
            String linkHref = ele.attr("href");
            String linkText = ele.text().trim();
            System.out.println("linkHref " + num + " : \t\t" + linkHref);
            System.out.println("linkText " + num + " : \t\t" + linkText);
        }
    }


    /**
     *
     * @param url
     * @param divClass      具体的div的特征   例如：div.ClassName    ul.ClassName
     * @param testMap       对应具体item的属性map
     * @return
     * @throws IOException
     */
    public static ArrayList<HashMap<String, String>> getSpicalProperty(String url, String divClass, HashMap<EleType, String> testMap) throws IOException {
        ArrayList<HashMap<String, String>> data = new ArrayList<>();

        Document doc2 = Jsoup.connect(url).get();
        Elements eles = doc2.select(divClass);
        for (Element ele : eles) {
            num++;
            HashMap<String, String> tempData = new HashMap<>();

            try {
                for (EleType et : testMap.keySet()) {
                    if (et == EleType.Attr) {
                        String attr = ele.attr(testMap.get(et));
                        tempData.put(testMap.get(et), attr);
                    } else if (et == EleType.Text) {
                        String text = ele.text().trim();
                        tempData.put(testMap.get(et), text);
                    }
                }
                data.add(tempData);

            } catch (Exception e) {
            }


        }
        return data;
    }


    /**
     *
     * @param url   具体url
     * @param divClass  具体的div的特征   例如：div.ClassName    ul.ClassName
     * @return
     * @throws IOException
     */
    public static ArrayList<HashMap<String, String>> getSpicalProperty(String url, String divClass) throws IOException {
        HashMap<EleType, String> testMap = new HashMap<>();
        testMap.put(EleType.Attr, "href");
        testMap.put(EleType.Text, "name");

        return getSpicalProperty(url, divClass, testMap);
    }

    public static void printMap(ArrayList<HashMap<String, String>> data) {
        if (data == null || data.size() == 0)
            return;

        for (HashMap<String, String> hm : data) {
            System.out.println("-----");
            for (String key : hm.keySet()) {
                System.out.println(key + "  :  " + hm.get(key));
            }
        }
    }


//    enum EleType {
//        Attr, Text
//    }


}
<fim_middle>