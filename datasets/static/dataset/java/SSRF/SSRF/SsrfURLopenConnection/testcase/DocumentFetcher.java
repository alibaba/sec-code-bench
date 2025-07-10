<filename>UniRideAndroid-master/app/src/main/java/com/sjsu/se195/uniride/DocumentFetcher.java<fim_prefix>

        package com.sjsu.se195.uniride;

import android.os.AsyncTask;

import org.w3c.dom.Document;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class DocumentFetcher extends AsyncTask<URL, Void, Document> {

    @Override
    protected Document doInBackground(URL... urls) {
        return getDocument(urls[0]);
    }

    private Document getDocument(URL url) {
        Document document = null;
        <fim_suffix>

        try {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoInput(true);
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = builder.parse(in);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return document;
    }
}
<fim_middle>