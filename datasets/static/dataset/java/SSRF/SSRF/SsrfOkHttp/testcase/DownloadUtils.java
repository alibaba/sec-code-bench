<filename>Android-Advert-SDK-master/advertlibrary/src/main/java/advert/sdk/com/advertlibrary/utils/DownloadUtils.java<fim_prefix>

        package advert.sdk.com.advertlibrary.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import advert.sdk.com.advertlibrary.R;
import advert.sdk.com.advertlibrary.bean.AdvertBean;
import advert.sdk.com.advertlibrary.engine.ProgressHelper;
import advert.sdk.com.advertlibrary.intf.OnDownloadListener;
import advert.sdk.com.advertlibrary.intf.OnGetBitmapByurlListener;
import advert.sdk.com.advertlibrary.intf.UIProgressResponseListener;

import static advert.sdk.com.advertlibrary.service.AdvertService.ADURLTEST1;
import static advert.sdk.com.advertlibrary.service.AdvertService.ADURLTEST2;
import static advert.sdk.com.advertlibrary.service.AdvertService.ADURLTEST3;
import static android.app.Notification.FLAG_AUTO_CANCEL;

/**
 */

public class DownloadUtils {
    /**
     * 模拟访问服务器数据的延迟
     *
     * @return
     */
    public static List<AdvertBean> getAdvertTest() {
        //假定已经访问了服务器了,返回AdvertBean
        AdvertBean banneradvertBean1 = new AdvertBean(3000, 1, 1, "http://oqv0h4wnb.bkt.clouddn.com/splash1.png", ADURLTEST1);//顶部
        AdvertBean banneradvertBean2=new AdvertBean(4000,2,0,"https://raw.githubusercontent.com/yipianfengye/android-adDialog/master/images/testImage1.png",ADURLTEST2);//底部
        AdvertBean insertadvertBean=new AdvertBean(5000,0,0,"http://oqv0h4wnb.bkt.clouddn.com/splash3.png",ADURLTEST3);//插屏
        List<AdvertBean>advertBeanList=new ArrayList<>();
        advertBeanList.add(banneradvertBean1);
        advertBeanList.add(banneradvertBean2);
        advertBeanList.add(insertadvertBean);
        return advertBeanList;
    }

    private static final int DOWNLOAD_SUCCESS = 0;
    private static final int DOWNLOAD_FAILED = 1;

    public static void getBitmapByPIcUrl(final String url, final OnGetBitmapByurlListener onGetBitmapByurlListener) {
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case DOWNLOAD_SUCCESS:
                        if (onGetBitmapByurlListener != null) {
                            onGetBitmapByurlListener.onGetBitmapSuccess((Bitmap) msg.obj);
                        }
                        break;
                    case DOWNLOAD_FAILED:
                        if (onGetBitmapByurlListener != null) {
                            onGetBitmapByurlListener.onGetBitmapFailed();
                        }
                        break;
                    default:
                        break;
                }
                super.handleMessage(msg);
            }
        };
        Request request = new Request.Builder()
                //下载地址
                .url(url)
                .build();
        //发送异步请求
        OkHttpClient client = getUnsafeOkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Message message = handler.obtainMessage();
                message.what = DOWNLOAD_FAILED;
                handler.sendMessage(message);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                //将返回结果转化为流
                InputStream inputStream = response.body().byteStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                Message message = handler.obtainMessage();
                message.what = DOWNLOAD_SUCCESS;
                message.obj = bitmap;
                handler.sendMessage(message);
            }
        });
    }

    /**
     * 根据传过来url创建文件
     */
    public static File getFile(String url) {
        File files = new File(Environment.getExternalStorageDirectory().getAbsoluteFile(), getFilePath(url));
        return files;
    }

    /**
     * 截取出url后面的apk的文件名
     */
    public static String getFilePath(String url) {
        return url.substring(url.lastIndexOf("/"), url.length());
    }

    /**
     * 通用下载方法
     *
     * @param url                        下载连接
     * @param listener                   下载结果监听
     * @param uiProgressResponseListener 下载进度回调
     */
    public static void download(final String url, final OnDownloadListener listener, final UIProgressResponseListener uiProgressResponseListener) {
        final File destFile = getFile(url);
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case DOWNLOAD_SUCCESS:
                        if (listener != null) {
                            listener.onDownloadSuccess(destFile);
                        }
                        break;
                    case DOWNLOAD_FAILED:
                        if (listener != null) {
                            listener.onDownloadFailed();
                        }
                        break;
                    default:
                        break;
                }
                super.handleMessage(msg);
            }
        };
        <fim_suffix>
        Request request = new Request.Builder()
                //下载地址
                .url(url)
                .build();
        if (uiProgressResponseListener != null) {
            ProgressHelper.addProgressResponseListener(client, uiProgressResponseListener).newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    Message message = handler.obtainMessage();
                    message.what = DOWNLOAD_FAILED;
                    handler.sendMessage(message);
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    //将返回结果转化为流，并写入文件
                    int len;
                    byte[] buf = new byte[2048];
                    InputStream inputStream = response.body().byteStream();
                    //可以在这里自定义路径

                    FileOutputStream fileOutputStream = new FileOutputStream(destFile);

                    while ((len = inputStream.read(buf)) != -1) {
                        fileOutputStream.write(buf, 0, len);
                    }
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    inputStream.close();
                    Message message = handler.obtainMessage();
                    message.what = DOWNLOAD_SUCCESS;
                    handler.sendMessage(message);
                }
            });
        } else {
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    Message message = handler.obtainMessage();
                    message.what = DOWNLOAD_FAILED;
                    handler.sendMessage(message);
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    //将返回结果转化为流，并写入文件
                    int len;
                    byte[] buf = new byte[2048];
                    InputStream inputStream = response.body().byteStream();
                    //可以在这里自定义路径
                    FileOutputStream fileOutputStream = new FileOutputStream(destFile);
                    while ((len = inputStream.read(buf)) != -1) {
                        fileOutputStream.write(buf, 0, len);
                    }
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    inputStream.close();
                    Message message = handler.obtainMessage();
                    message.what = DOWNLOAD_SUCCESS;
                    handler.sendMessage(message);
                }
            });
        }
    }

    /**
     * 获取能够访问不安全连接的HttpClient
     *
     * @return
     */
    public static OkHttpClient getUnsafeOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] chain,
                        String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] chain,
                        String authType) throws CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            }};
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts,
                    new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext
                    .getSocketFactory();
            OkHttpClient okHttpClient = new OkHttpClient();
            okHttpClient.setSslSocketFactory(sslSocketFactory);
            okHttpClient.setHostnameVerifier(new HostnameVerifier() {

                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static Notification.Builder notifivationbuilder;
    static NotificationManager manager;
    static Notification notification;

    public static void showProgressnotifivation(Context context, int ratio) {
        if (notifivationbuilder == null) {
            notifivationbuilder = new Notification.Builder(context);
            notifivationbuilder.setSmallIcon(R.drawable.close);//设置图标
            notifivationbuilder.setProgress(100, 0, false);
            notifivationbuilder.setTicker("下载中");//手机状态栏的提示
            notifivationbuilder.setContentTitle("下载中");//设置标题
            notifivationbuilder.setContentText("下载中");//设置通知内容
            notifivationbuilder.setWhen(System.currentTimeMillis());//设置通知时间
            manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notifivationbuilder.setDefaults(Notification.DEFAULT_LIGHTS);//设置指示灯
        } else {
            notifivationbuilder.setProgress(100, ratio, false);
        }
        notification = notifivationbuilder.build();
        notification.flags = FLAG_AUTO_CANCEL;
        manager.notify(3, notification);

    }

}
<fim_middle>