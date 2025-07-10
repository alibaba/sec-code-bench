<filename>Recreation-master/app/src/main/java/com/fanyafeng/recreation/network/NetUtil.java<fim_prefix>

        package com.fanyafeng.recreation.network;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;


import com.fanyafeng.recreation.R;
import com.fanyafeng.recreation.util.DevicePreferences;
import com.fanyafeng.recreation.util.MyUtils;
import com.fanyafeng.recreation.util.StringUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by zgw on 16/5/7 15:24.
 */
public class NetUtil {

    public static final String STATE_OK = "ok";

    public static final String STATE_FAILED = "failed";

    public static final String HEADER_KEY_CONTENT_ENCODING = "content-encoding";

    public static final String CLIENT_USER_AGENT = "Android vino";

    public static final String USER_AGENT = "User-Agent";

    public static final String HEADER_KEY_ACCEPT_ENCODING = "Accept-encoding";

    public static final String GZIP = "gzip";

    private OkHttpClient mOkHttpClient;

    private static final String TAG = "NetUtils";

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static String toJSON(Map<String, String> map) {
        JSONObject o = new JSONObject();

        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                try {
                    o.put(entry.getKey(), entry.getValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return o.toString();
    }

    public static String httpPostUtil(Context context, String url, Map<String, String> params) {
        <fim_suffix>
        if (!networkAvailable(context)) {
            return null;
        }
        String result;

        FormBody.Builder builder = new FormBody.Builder();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!StringUtil.isNullOrEmpty(entry.getValue())) {
                    try {
                        builder.addEncoded(entry.getKey(), URLEncoder.encode(entry.getValue(), "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } else {
                    builder.addEncoded(entry.getKey(), "");
                }
            }
        }

        Request.Builder requestBuilder = new Request.Builder()
                .header(USER_AGENT, getHeader(context))
                .header(HEADER_KEY_ACCEPT_ENCODING, "gzip")
                .url(url)
                .post(builder.build());

//        Account account = new AccountManager(context).getAccount();

//        if (account.isLogined()) {
//            setTokenHeader(requestBuilder, account.getToken(), url, new DevicePreferences(context).getTokenId());
//        } else {
        setTokenHeader(requestBuilder, null, url, new DevicePreferences(context).getUADeviceID());
//        }

        Request request = requestBuilder.build();

        Response response = null;
        try {
            response = mOkHttpClient.newCall(request).execute();
            Log.d(TAG, "The request httpPostUtil: " + url + " " + response.code());
            if (response.isSuccessful()) {
                if ("gzip".equals(response.header(HEADER_KEY_CONTENT_ENCODING))) {
                    result = decompress(response.body().byteStream());
                } else {
                    result = response.body().string();
                }
            } else {
                result = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            result = null;
        } catch (Exception e) {
            e.printStackTrace();
            result = null;
        }
        Log.d(TAG, "The response httpPostUtil: " + url + " the result: " + result);
        return result;
    }

    public static String httpPostUtil(Context context, String url, JSONObject jsonObject) {
        if (!networkAvailable(context)) {
            return null;
        }
        String result;

        RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());

        Request.Builder requestBuilder = new Request.Builder()
                .header(USER_AGENT, getHeader(context))
                .header(HEADER_KEY_ACCEPT_ENCODING, "gzip")
                .url(url)
                .post(requestBody);

//        Account account = new AccountManager(context).getAccount();

//        if (account.isLogined()) {
//            setTokenHeader(requestBuilder, account.getToken(), url, new DevicePreferences(context).getTokenId());
//        } else {
        setTokenHeader(requestBuilder, null, url, new DevicePreferences(context).getUADeviceID());
//        }

        Request request = requestBuilder.build();

        Response response = null;
        try {
            response = mOkHttpClient.newCall(request).execute();
            Log.d(TAG, "The request httpPostUtil: " + url + " " + response.code());
            if (response.isSuccessful()) {
                if ("gzip".equals(response.header(HEADER_KEY_CONTENT_ENCODING))) {
                    result = decompress(response.body().byteStream());
                } else {
                    result = response.body().string();
                }
            } else {
                result = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            result = null;
        } catch (Exception e) {
            e.printStackTrace();
            result = null;
        }
        Log.d(TAG, "The response httpPostUtil: " + url + " the result: " + result);
        return result;
    }

    private static String preEncode(String url) {
//        Uri uri = Uri.parse(url);
//
//        String params = "";
//
//        System.out.println("encode:"+uri.getQuery());
//        System.out.println("encode:"+uri.getEncodedQuery());
//
//        String res = uri.getScheme() + "://" + uri.getHost() + uri.getPath() + "?" + params;
//
//
//        try {
//            URL u = new URL(url);
//            URI i = new URI(u.getProtocol(), u.getUserInfo(), u.getHost(), u.getPort(), u.getPath(), u.getQuery(), u.getRef());
//
//            u = i.toURL();
//
//            System.out.println("encode sam:" + u.toString());
//            System.out.println("encode zgw:" + Uri.encode(url, "@#&=*-_.,:!?()/~'%"));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
                    return Uri.encode(url, "@#&=*-_.,:!?()/~'%");
                }

                public static String httpGetUtil(Context context, String url) {
                    if (!networkAvailable(context)) {
                        return null;
                    }
                    String result = null;

                    Request.Builder requestBuilder = new Request.Builder()
                            .url(preEncode(url))
//                .url(url.replace("+", "%2B"))//FIXME: hmac issue
                            .get()
                            .header(USER_AGENT, getHeader(context))
//                .header("Host","m2.qiushibaike.com")
                            .header(HEADER_KEY_ACCEPT_ENCODING, GZIP);

//        Account account = new AccountManager(context).getAccount();
//
//        if (account.isLogined()) {
//            setTokenHeader(requestBuilder, account.getToken(), url, new DevicePreferences(context).getTokenId());
//        } else {
                    setTokenHeader(requestBuilder, null, url, new DevicePreferences(context).getUADeviceID());
//        }

                    Request request = requestBuilder.build();
                    Response response;

                    try {
                        response = mOkHttpClient.newCall(request).execute();
                        Log.d(TAG, "The request httpGetUtil: " + url + " " + response.code());
                        if (response.isSuccessful()) {
                            if ("gzip".equals(response.header(HEADER_KEY_CONTENT_ENCODING))) {
                                result = decompress(response.body().byteStream());
                            } else {
                                result = response.body().string();
                            }
                        } else {
                            result = null;
                        }
                    } catch (Exception e) {

                    }
                    return result;
                }

                private static void setTokenHeader(Request.Builder builder, String token, String url, String deviceID) {
                    Map<String, String> headers = ClientHMAC.hmacInfos(url, deviceID, token);

                    if (headers != null) {
                        for (Map.Entry<String, String> entry : headers.entrySet()) {
                            builder.header(entry.getKey(), entry.getValue());
                        }
                    }
                }

                private static String decompress(InputStream is) {
                    StringBuffer sb = new StringBuffer();

                    try {
                        GZIPInputStream gis = new GZIPInputStream(is);

                        InputStreamReader isr = new InputStreamReader(gis, "UTF-8");
                        java.io.BufferedReader br = new java.io.BufferedReader(isr);
                        sb = new StringBuffer();
                        String tempbf;
                        while ((tempbf = br.readLine()) != null) {
                            sb.append(tempbf);
                            sb.append("\n");
                        }
                        isr.close();
                        gis.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return sb.toString();
                }

                public static boolean networkAvailable(Context context) {
                    if (context == null) {
                        return false;
                    }
                    ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    if (connectivityManager == null) {
                        return false;
                    }
                    NetworkInfo info = connectivityManager.getActiveNetworkInfo();

                    if (info != null) {
                        return info.isAvailable();
                    }
                    return false;
                }

                public static String getHeader(Context context) {
                    PackageInfo pi = null;
                    try {
                        pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
//        return CLIENT_USER_AGENT
//                + "/" + MyUtils.getChannel(context) + " " + pi.versionName + "(" + pi.versionCode + ")"
//                + "/" + new DevicePreferences(context).getUADeviceID();

                    String versionName = null;

                    if (pi == null) {
                        versionName = context.getString(R.string.version_name);
                    } else {
                        versionName = pi.versionName;
                    }
                    return CLIENT_USER_AGENT + "/" + new DevicePreferences(context).getUADeviceID() + "/"
                            + versionName + "/" + cityCode;
                }

                private static final String cityCode = "0";
            }
<fim_middle>