<filename>coolweather-master/app/src/main/java/com/example/coolweather/util/HttpUtil.java<fim_prefix>

        package com.example.coolweather.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class HttpUtil {

    public static void sendOKHttpRequest(String adress, okhttp3.Callback callback){
        <fim_suffix>
        Request request = new Request.Builder().url(adress).build();
        client.newCall(request).enqueue(callback);
    }
}
<fim_middle>