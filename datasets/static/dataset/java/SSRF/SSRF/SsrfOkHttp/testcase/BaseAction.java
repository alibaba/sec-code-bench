<filename>cloudtalk-master/android/IMLib/src/main/java/com/zhangwuji/im/server/network/BaseAction.java<fim_prefix>

        package com.zhangwuji.im.server.network;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zhangwuji.im.DB.sp.LoginSp;
import com.zhangwuji.im.DB.sp.SystemConfigSp;
import com.zhangwuji.im.server.network.http.HttpException;
import com.zhangwuji.im.server.utils.json.JsonMananger;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class BaseAction {
    public static final String RootDOMAIN = SystemConfigSp.instance().getStrConfig(SystemConfigSp.SysCfgDimension.LOGINSERVER);
    protected Context mContext;

    private Handler mWorkHandler;
    private HandlerThread mWorkThread;
    static Handler mHandler;

    public static abstract class ResultCallback<T> {

        public static class Result<T> {
            public T t;
        }

        public ResultCallback() {
        }

        /**
         * 成功时回调。
         *
         * @param t 已声明的类型。
         */
        public abstract void onSuccess(T t);

        /**
         * 错误时回调。
         *
         * @param errString 错误提示
         */
        public abstract void onError(String errString);


        public void onFail(final String errString) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    onError(errString);
                }
            });
        }

        public void onCallback(final T t) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    onSuccess(t);
                }
            });
        }
    }

    public String listToString(List<String> stringList) {
        if (stringList == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        boolean flag = false;
        for (String string : stringList) {
            if (flag) {
                result.append(",");
            } else {
                flag = true;
            }
            result.append(string);
        }
        return result.toString();
    }

    //获取返回数据的data内容
    public String getData(String t) {
        try {
            JSONObject obj = JSON.parseObject(t.toString());
            return obj.getString("data");
        } catch (Exception ee) {
            return "";
        }
    }

    public int getCode(String t) {
        try {
            JSONObject obj = JSON.parseObject(t.toString());
            return Integer.parseInt(obj.getString("code"));
        } catch (Exception ee) {
            return -1;
        }
    }

    //获取返回的数据code
    public int getErrorCode(String t) {
        try {
            JSONObject obj = JSON.parseObject(t.toString());
            return Integer.parseInt(obj.getString("error"));
        } catch (Exception ee) {
            return -1;
        }
    }

    /**
     * 构造方法
     *
     * @param context 上下文
     */
    public BaseAction(Context context) {
        this.mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * JSON转JAVA对象方法
     *
     * @param json json
     * @param cls  class
     * @throws HttpException
     */
    public <T> T jsonToBean(String json, Class<T> cls) throws HttpException {
        return JsonMananger.jsonToBean(json, cls);
    }

    /**
     * JSON转JAVA数组方法
     *
     * @param json json
     * @param cls  class
     * @throws HttpException
     */
    public <T> List<T> jsonToList(String json, Class<T> cls) throws HttpException {
        return JsonMananger.jsonToList(json, cls);
    }

    /**
     * JAVA对象转JSON方法
     *
     * @param obj object
     * @throws HttpException
     */
    public String BeanTojson(Object obj) throws HttpException {
        return JsonMananger.beanToJson(obj);
    }


    /**
     * 获取完整URL方法
     *
     * @param url url
     */
    protected String getURL(String url, String... params) {
        StringBuilder urlBilder = new StringBuilder(RootDOMAIN).append(url);
        if (params != null) {
            for (String param : params) {
                if (!urlBilder.toString().endsWith("/")) {
                    urlBilder.append("/");
                }
                urlBilder.append(param);
            }
        }
        return urlBilder.toString();
    }


    /**
     * 发送get请求 同步操作
     *
     * @param url 请求url
     * @return
     */
    public String IMHttpGet(String url) {
        final Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        String body = null;
        OkHttpClient okHttpClient = genericClient();
        try {
            body = okHttpClient.newCall(request).execute().body().string();
        } catch (IOException e) {
        }
        return body;
    }

    /**
     * 发送post请求 同步操作
     *
     * @param url    请求url地址
     * @param params post参数
     * @return
     */
    public String IMHttpPost(String url, Map<String, String> params) {
        FormBody.Builder fromparams = new FormBody.Builder();
        Set<String> keySet = params.keySet();
        for (String key : keySet) {
            fromparams.add(key, params.get(key));
        }
        Request request = new Request.Builder()
                .url(url)
                .post(fromparams.build())
                .build();

        String body = null;
        OkHttpClient okHttpClient = genericClient();
        try {
            body = okHttpClient.newCall(request).execute().body().string();
        } catch (IOException e) {
        }
        return body;

    }

    /**
     * 发送get请求 异步回调
     *
     * @param url
     * @param callback
     */
    public void IMHttpGetCallBack(String url, final ResultCallback<String> callback) {
        try {
            <fim_suffix>
            final Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (callback != null) {
                        callback.onFail(e.getMessage());
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (callback != null) {
                        callback.onCallback(response.body().string());
                    }
                }
            });
        } catch (Exception e) {
        }
    }

    /**
     * 发送post 请求 异步回调
     *
     * @param url
     * @param params
     * @param callback
     */
    public void IMHttpPostCallBack(String url, Map<String, String> params, final ResultCallback<String> callback) {
        FormBody.Builder fromparams = new FormBody.Builder();
        Set<String> keySet = params.keySet();
        for (String key : keySet) {
            fromparams.add(key, params.get(key));
        }
        Request request = new Request.Builder()
                .url(url)
                .post(fromparams.build())
                .build();

        OkHttpClient okHttpClient = genericClient();
        try {
            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (callback != null) {
                        String msg = e.getMessage();

                        callback.onFail(msg);
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                     if (callback != null) {
                        String result = response.body().string();
                        callback.onCallback(result);
                    }
                }
            });
        } catch (Exception e) {
        }
    }

    public void IMHttpPostCallBack2(String url, Map<String, String> params, final ResultCallback<String> callback) {
        FormBody.Builder fromparams = new FormBody.Builder();
        Set<String> keySet = params.keySet();
        for (String key : keySet) {
            fromparams.add(key, params.get(key));
        }
        Request request = new Request.Builder()
                .url(url)
                .post(fromparams.build())
                .build();

        OkHttpClient okHttpClient = genericClient2();
        try {
            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (callback != null) {
                        String msg = e.getMessage();

                        callback.onFail(msg);
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (callback != null) {
                        String result = response.body().string();
                        callback.onCallback(result);
                    }
                }
            });
        } catch (Exception e) {
        }
    }

    public void IMHttpPostFileCallBack(String url, RequestBody requestBody, final ResultCallback<String> callback) {
        Request request = new Request.Builder().url(url).post(requestBody).build();
        OkHttpClient okHttpClient = genericClient();
        try {
            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (callback != null) {
                        callback.onFail(e.getMessage());
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (callback != null) {
                        callback.onCallback(response.body().string());
                    }
                }
            });
        } catch (Exception e) {
        }
    }

    public static OkHttpClient genericClient() {
        final String token = LoginSp.instance().getLoginIdentity() == null ? "" : LoginSp.instance().getLoginIdentity().getToken();
            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .connectTimeout(3 * 60, TimeUnit.SECONDS)
                    .readTimeout(3 * 60, TimeUnit.SECONDS)
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request request = chain.request()
                                    .newBuilder()
                                    .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                                .addHeader("token", token)
                                    .addHeader("appid", SystemConfigSp.instance().getStrConfig(SystemConfigSp.SysCfgDimension.APPID))
                                    .build();
                            return chain.proceed(request);
                        }

                    })
                    .build();

            return httpClient;
        }

        public static OkHttpClient genericClient2() {
            final String token = LoginSp.instance().getLoginIdentity() == null ? "" : LoginSp.instance().getLoginIdentity().getToken();
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(3 * 60, TimeUnit.SECONDS)
                .readTimeout(3 * 60, TimeUnit.SECONDS)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request()
                                .newBuilder()
                                .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                                .addHeader("token", token)
                                .addHeader("appid", SystemConfigSp.instance().getStrConfig(SystemConfigSp.SysCfgDimension.APPID))
                                .build();
                        return chain.proceed(request);
                    }

                })
                .build();

        return httpClient;
    }


    public String uploadBigImage(String strUrl, byte[] bytes, String fileName) {
        List<String> list = new ArrayList<String>(); // 要上传的文件名,如：d:\haha.doc.你要实现自己的业务。我这里就是一个空list.
        list.add(fileName);
        try {
            String BOUNDARY = "---------7d4a6d158c9"; // 定义数据分隔线
            URL url = new URL(strUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
            conn.setRequestProperty("Charsert", "UTF-8");
            conn.setRequestProperty("Content-Type",
                    "multipart/form-data; boundary=" + BOUNDARY);

            OutputStream out = new DataOutputStream(conn.getOutputStream());
            byte[] end_data = ("\r\n--" + BOUNDARY + "--\r\n").getBytes();// 定义最后数据分隔线
            int leng = list.size();
            for (int i = 0; i < leng; i++) {
                String fname = list.get(i);
                File file = new File(fname);
                StringBuilder sb = new StringBuilder();
                sb.append("--");
                sb.append(BOUNDARY);
                sb.append("\r\n");
                sb.append("Content-Disposition: form-data;name=\"file" + i
                        + "\";filename=\"" + file.getName() + "\"\r\n");
            sb.append("Content-Type:application/octet-stream\r\n\r\n");

            byte[] data = sb.toString().getBytes();
            out.write(data);
            out.write(bytes);
            out.write("\r\n".getBytes()); // 多个文件时，二个文件之间加入这个
        }
        out.write(end_data);
        out.flush();
        out.close();

        // 定义BufferedReader输入流来读取URL的响应
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                conn.getInputStream()));
        String line = null;
        while ((line = reader.readLine()) != null) {
            // todo eric
            /*
             * {"error_code":0,"error_msg":
             * "成功","path":"g0/000/000/1410706133246550_140184328214.jpg"
             * ,"url":
             * "http://122.225.68.125:8001/g0/000/000/1410706133246550_140184328214.jpg"
             * }
             */
            JSONObject object = JSON.parseObject(line);
            return object.getString("url");
        }

    } catch (Exception e) {
        System.out.println("pic#发送POST请求出现异常！" + e);
        e.printStackTrace();
    }

        return "";
    }


}
<fim_middle>