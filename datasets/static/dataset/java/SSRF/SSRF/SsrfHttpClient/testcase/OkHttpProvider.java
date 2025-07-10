<filename>OkHttpLib-master/OkHttpLibSDK/src/main/java/com/xingen/okhttplib/internal/okhttp/OkHttpProvider.java<fim_prefix>

        package com.xingen.okhttplib.internal.okhttp;

import com.xingen.okhttplib.config.NetConfig;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * Created by ${xinGen} on 2018/1/19.
 */

public class OkHttpProvider {
    public  static OkHttpClient createOkHttpClient(NetConfig netConfig){
        OkHttpClient.Builder builder=new OkHttpClient.Builder();
        if (netConfig!=null){
            if (netConfig.isLog()){
                builder.addInterceptor(createLogInterceptor());
            }
            if (netConfig.getCommonHeaders().size()>0){
                builder.addInterceptor(createHeaderInterceptor(netConfig.getCommonHeaders()));
            }
        }else{
            setRequestTime(builder);
        }
        return builder.build();
    }

    private static  void setRequestTime(OkHttpClient.Builder builder){
        builder.connectTimeout(60, TimeUnit.SECONDS);
        builder.readTimeout(60,TimeUnit.SECONDS);
        builder.writeTimeout(60,TimeUnit.SECONDS);
    }
    /**
     * 共同header的烂机器
     * @param headers
     * @return
     */
    private  static HeaderInterceptor createHeaderInterceptor(Map<String ,String> headers){
        return  new HeaderInterceptor(headers);
    }
    /**
     * 日志的拦截器
     * @return
     */
    private static HttpLoggingInterceptor createLogInterceptor (){
        HttpLoggingInterceptor loggingInterceptor =new  HttpLoggingInterceptor();
        //打印一次请求的全部信息
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return loggingInterceptor  ;
    }

    /**
     * 创建form表单上传的内容格式
     * @return
     */
    public static MediaType createFormType(){
        MediaType CONTENT_TYPE = MediaType.parse("application/x-www-form-urlencoded");
        return CONTENT_TYPE;
    }
    /**
     * get请求
     *
     * @param url
     * @return
     */
    public static Request createGetRequest(String url) {
        return createGetRequest(url, null);
    }

    /**
     * get请求
     *
     * @param url
     * @param headers
     * @return
     */
    public static Request createGetRequest(String url, Map<String, String> headers) {
        return createRequest(url, null, headers);
    }

    /**
     * post请求
     *
     * @param url
     * @param requestBody
     * @return
     */
    public static Request createPostRequest(String url, RequestBody requestBody) {
        return createRequest(url, requestBody, null);
    }

    /**
     * post请求
     *
     * @param url
     * @param requestBody
     * @param headers
     * @return
     */
    public static Request createPostRequest(String url, RequestBody requestBody, Map<String, String> headers) {
        return createRequest(url, requestBody, headers);
    }

    /**
     * OkHttp 的Request
     *
     * @param url
     * @param requestBody
     * @param headers
     * @return
     */
    private static Request createRequest(String url, RequestBody requestBody, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder();
        builder.url(url);
        if (requestBody != null) {
            builder.post(requestBody);
        }
        if (headers != null) {
            Set<Map.Entry<String, String>> set = headers.entrySet();
            for (Map.Entry<String, String> entry : set) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }

    /**
     * 创建一个call,可用于取消 请求
     *
     * @param okHttpClient
     * @param request
     * @return
     */
    public static Call createCall(OkHttpClient okHttpClient, Request request) {
        <fim_suffix>
        Call call = okHttpClient.newCall(request);
        return call;
    }

    /**
     * 异步执行
     *
     * @param call
     * @param responseCallback
     */
    public static void executeAsynchronous(Call call, Callback responseCallback) {
        call.enqueue(responseCallback);
    }

    /**
     * 同步执行
     *
     * @param call
     * @return
     */
    public static Response executeSynchronous(Call call) throws  IOException{
        Response response = call.execute();
        return response;
    }

}
<fim_middle>