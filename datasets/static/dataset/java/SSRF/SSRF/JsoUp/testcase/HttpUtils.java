<filename>CyberBox-main/Skay_Exp/src/tools/HttpUtils.java<fim_prefix>

package tools;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import javax.net.ssl.*;
import java.io.*;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class HttpUtils {
	/**
     * ����ʱʱ��
     */
    private static final int TIME_OUT = 15000;

    /**
     * Https����
     */
    private static final String HTTPS = "https";

    /**
     * Content-Type
     */
    private static final String CONTENT_TYPE = "Content-Type";

    /**
     * ���ύ��ʽContent-Type
     */
    private static final String FORM_TYPE = "application/x-www-form-urlencoded;charset=UTF-8";

    /**
     * JSON�ύ��ʽContent-Type
     */
    private static final String JSON_TYPE = "application/json;charset=UTF-8";

    /**
     * ����Get����
     * 
     * @param url ����URL
     * @return HTTP��Ӧ����
     * @throws IOException �����쳣ʱ�׳����ɵ����ߴ���
     */
    public static Response get(String url) throws IOException {
        return get(url, null);
    }

    /**
     * ����Get����
     * 
     * @param url ����URL
     * @param headers ����ͷ����
     * @return HTTP��Ӧ����
     * @throws IOException �����쳣ʱ�׳����ɵ����ߴ���
     */
    public static Response get(String url, Map<String, String> headers) throws IOException {
        if (null == url || url.isEmpty()) {
            throw new RuntimeException("The request URL is blank.");
        }

        // �����Https����
        if (url.startsWith(HTTPS)) {
            getTrust();
        }

<fim_suffix>

        Connection connection = Jsoup.connect(url);
        connection.method(Method.GET);
        connection.timeout(TIME_OUT);
        connection.ignoreHttpErrors(true);
        connection.ignoreContentType(true);
        connection.maxBodySize(0);

        if (null != headers) {
            connection.headers(headers);
        }

        Response response = connection.execute();
        return response;
    }

    /**
     * ����JSON��ʽ����POST����
     * 
     * @param url ����·��
     * @param params JSON��ʽ�������
     * @return HTTP��Ӧ����
     * @throws IOException �����쳣ʱ�׳����ɵ����ߴ���
     */
    public static Response post(String url, String params) throws IOException {
        return doPostRequest(url, null, params);
    }

    /**
     * ����JSON��ʽ����POST����
     * 
     * @param url ����·��
     * @param headers ����ͷ�����õĲ���
     * @param params JSON��ʽ�������
     * @return HTTP��Ӧ����
     * @throws IOException �����쳣ʱ�׳����ɵ����ߴ���
     */
    public static Response post(String url, Map<String, String> headers, String params) throws IOException {
        return doPostRequest(url, headers, params);
    }

    /**
     * �ַ�������post����
     * 
     * @param url ����URL��ַ
     * @param paramMap �����ַ�����������
     * @return HTTP��Ӧ����
     * @throws IOException �����쳣ʱ�׳����ɵ����ߴ���
     */
    public static Response post(String url, Map<String, String> paramMap) throws IOException {
        return doPostRequest(url, null, paramMap, null);
    }

    /**
     * ������ͷ����ͨ���ύ��ʽpost����
     * 
     * @param headers ����ͷ����
     * @param url ����URL��ַ
     * @param paramMap �����ַ�����������
     * @return HTTP��Ӧ����
     * @throws IOException �����쳣ʱ�׳����ɵ����ߴ���
     */
    public static Response post(Map<String, String> headers, String url, Map<String, String> paramMap) throws IOException {
        return doPostRequest(url, headers, paramMap, null);
    }

    /**
     * ���ϴ��ļ���post����
     * 
     * @param url ����URL��ַ
     * @param paramMap �����ַ�����������
     * @param fileMap �����ļ���������
     * @return HTTP��Ӧ����
     * @throws IOException �����쳣ʱ�׳����ɵ����ߴ���
     */
    public static Response post(String url, Map<String, String> paramMap, Map<String, File> fileMap) throws IOException {
        return doPostRequest(url, null, paramMap, fileMap);
    }

    /**
     * ������ͷ���ϴ��ļ�post����
     * 
     * @param url ����URL��ַ
     * @param headers ����ͷ����
     * @param paramMap �����ַ�����������
     * @param fileMap �����ļ���������
     * @return HTTP��Ӧ����
     * @throws IOException �����쳣ʱ�׳����ɵ����ߴ���
     */
    public static Response post(String url, Map<String, String> headers, Map<String, String> paramMap, Map<String, File> fileMap) throws IOException {
        return doPostRequest(url, headers, paramMap, fileMap);
    }

    /**
     * ִ��post����
     * 
     * @param url ����URL��ַ
     * @param headers ����ͷ
     * @param jsonParams ����JSON��ʽ�ַ�������
     * @return HTTP��Ӧ����
     * @throws IOException �����쳣ʱ�׳����ɵ����ߴ���
     */
    private static Response doPostRequest(String url, Map<String, String> headers, String jsonParams) throws IOException {
        if (null == url || url.isEmpty()) {
            throw new RuntimeException("The request URL is blank.");
        }

        // �����Https����
        if (url.startsWith(HTTPS)) {
            getTrust();
        }
//        System.out.println(url);
        Connection connection = Jsoup.connect(url);
        connection.method(Method.POST);
        connection.timeout(TIME_OUT);
        connection.ignoreHttpErrors(true);
        connection.ignoreContentType(true);
        connection.maxBodySize(0);

        if (null != headers) {
            connection.headers(headers);
        }

        connection.header(CONTENT_TYPE, JSON_TYPE);
        connection.requestBody(jsonParams);

        Response response = connection.execute();
        return response;
    }

    /**
     * ��ͨ����ʽ����POST����
     * 
     * @param url ����URL��ַ
     * @param headers ����ͷ
     * @param paramMap �����ַ�����������
     * @param fileMap �����ļ���������
     * @return HTTP��Ӧ����
     * @throws IOException �����쳣ʱ�׳����ɵ����ߴ���
     */
    private static Response doPostRequest(String url, Map<String, String> headers, Map<String, String> paramMap, Map<String, File> fileMap) throws IOException {
        if (null == url || url.isEmpty()) {
            throw new RuntimeException("The request URL is blank.");
        }

        // �����Https����
        if (url.startsWith(HTTPS)) {
            getTrust();
        }

        Connection connection = Jsoup.connect(url);
        connection.method(Method.POST);
        connection.timeout(TIME_OUT);
        connection.ignoreHttpErrors(true);
        connection.ignoreContentType(true);
        connection.maxBodySize(0);

        if (null != headers) {
            connection.headers(headers);
        }

        // �ռ��ϴ��ļ�������������ȫ���ر�
        List<InputStream> inputStreamList = null;
        try {

            // ����ļ�����
            if (null != fileMap && !fileMap.isEmpty()) {
                inputStreamList = new ArrayList<InputStream>();
                InputStream in = null;
                File file = null;
                for (Entry<String, File> e : fileMap.entrySet()) {
                    file = e.getValue();
                    in = new FileInputStream(file);
                    inputStreamList.add(in);
                    connection.data(e.getKey(), file.getName(), in);
                }
            }

            // ��ͨ���ύ��ʽ
            else {
                connection.header(CONTENT_TYPE, FORM_TYPE);
            }

            // ����ַ��������
            if (null != paramMap && !paramMap.isEmpty()) {
                connection.data(paramMap);
            }

            Response response = connection.execute();
            return response;
        }

        // �ر��ϴ��ļ���������
        finally {
            closeStream(inputStreamList);
        }
    }

    /**
     * ����
     * 
     * @param streamList ������
     */
    private static void closeStream(List<? extends Closeable> streamList) {
        if (null != streamList) {
            for (Closeable stream : streamList) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * ��ȡ����������
     */
    private static void getTrust() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[] { new X509TrustManager() {

                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}

                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            } }, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
<fim_middle>