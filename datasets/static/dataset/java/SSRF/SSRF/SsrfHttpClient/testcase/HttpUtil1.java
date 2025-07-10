<filename>youhuigou123-master/shop_util/src/main/java/data/http/HttpUtil1.java<fim_prefix>

        package data.http;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class HttpUtil {

    Logger logger = LogManager.getLogger(HttpUtil.class);

    public Boolean isRequestSuccessful(HttpResponse httpresponse){
        return httpresponse.getStatusLine().getStatusCode()==200;
    }

    public HttpResponse HttpGet(Map<String,String> paramMap,String url) throws Exception {

        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = null;

        //封装请求参数
        List<NameValuePair> params = new ArrayList();
        Set<String> set = paramMap.keySet();
        Iterator<String> iterator = set.iterator();
        while (iterator.hasNext()) {
            String next = iterator.next();
            params.add(new BasicNameValuePair(next,paramMap.get(next)));
        }
        //转换为键值对
        String str = EntityUtils.toString(new UrlEncodedFormEntity(params, Consts.UTF_8));
        logger.debug(str);
        //创建Get请求
        HttpGet httpGet = new HttpGet(url+"?"+str);
        //请求头
        Header header = new BasicHeader("Content-type", "application/json");
        httpGet.addHeader(header);
        //执行Get请求，
        response = httpClient.execute(httpGet);
        logger.debug(response.getEntity());
        return response;
    }


    public HttpResponse HttpPost(Map<String,String> requestParamMap, String url) throws Exception{
//        Map<String,String> personMap = new HashMap<String,String>();
//        personMap.put("param1",param1);
//        personMap.put("param1",param2);
        List<NameValuePair> list = new LinkedList<NameValuePair>();
        for(Entry<String,String> entry:requestParamMap.entrySet()){
            list.add(new BasicNameValuePair(entry.getKey(),entry.getValue()));
        }
        <fim_suffix>

        HttpPost httpPost = new HttpPost(url);

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(list,"utf-8");
        httpPost.setEntity(formEntity);
        HttpClient httpCient = HttpClients.createDefault();
        HttpResponse httpresponse = null;
        try{
            httpresponse = httpCient.execute(httpPost);
//            HttpEntity httpEntity = httpresponse.getEntity();
//            String response = EntityUtils.toString(httpEntity, "utf-8");
            return httpresponse;
        }catch(ClientProtocolException e){
            System.out.println("http请求失败，uri{},exception{}");
        }catch(IOException e){
            System.out.println("http请求失败，uri{},exception{}");
        }
        return null;
    }

}
<fim_middle>