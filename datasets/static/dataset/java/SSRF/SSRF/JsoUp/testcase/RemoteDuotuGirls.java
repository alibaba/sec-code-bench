<filename>BigGirl-master/app/src/main/java/com/yibao/biggirl/model/duotu/RemoteDuotuGirls.java<fim_prefix>

package com.yibao.biggirl.model.duotu;

import android.annotation.SuppressLint;

import com.yibao.biggirl.model.girl.Girl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Author：Sid
 * Des：${TODO}
 * Time:2017/4/26 00:52
 */
public class RemoteDuotuGirls
        implements DuotuDataSource {


    @SuppressLint("CheckResult")
    @Override
    public void getDuotu(String url, int page, LoadDuotuCallback callback) {
        String baseUrl = url + page + ".html";

        Observable.just(baseUrl).subscribeOn(Schedulers.io()).map(s -> {

            List<Girl> girls = new ArrayList<>();
            try {

<fim_suffix>

                Document doc = Jsoup.connect(baseUrl).timeout(10000).get();
                Element total = doc.select("div.boxs").first();
                Elements items = total.select("li");
                for (int i = 1; i < items.size(); i++) {
                    Element element = items.get(i);
                    Girl girl = new Girl(element.select("img").first().attr("src"));
                    girl.setLink(element.select("a[href]").last().attr("href"));
                    girls.add(girl);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return girls;
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(callback::onLoadDatas);
    }

    @SuppressLint("CheckResult")
    @Override
    public void getDuotuList(String url, int page, DuotuDataSource.LoadDuotuCallback callback) {
//                截取字符串第三个小数点之前的字符串
//        String s="30.6.9";s.substring(0,s.indexOf(".",s.indexOf(".")+2));
        String baseUrl = getBaseUrl(url, page);
        Observable.just(baseUrl).subscribeOn(Schedulers.io()).map(s -> {
            List<Girl> girls = new ArrayList<>();
            try {
                Document doc = Jsoup.connect(baseUrl).timeout(10000).get();
                Element total = doc.select("div.content").first();
                Elements allElements = total.getAllElements();

                Elements items = total.select("p");
                for (Element element : allElements) {
                    Girl girl = new Girl(element.select("img").first().attr("src"));
                    girls.add(girl);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return girls;
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(callback::onLoadDatas);
    }

    private String getBaseUrl(String url, int page) {
        String mBaseUrl;
        if (page == 1) {
            mBaseUrl = url;
        } else {
            String str = url.substring(0, url.length() - 5);
            mBaseUrl = str + "_" + page + ".html";
        }
        return mBaseUrl;
    }


}
<fim_middle>