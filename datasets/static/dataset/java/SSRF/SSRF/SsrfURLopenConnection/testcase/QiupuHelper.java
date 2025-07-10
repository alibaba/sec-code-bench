<filename>3DHome-master/Market/src/main/java/com/borqs/market/utils/QiupuHelper.java<fim_prefix>

        package com.borqs.market.utils;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.borqs.market.listener.DownloadListener;
import com.funyoung.androidfacade.AndroidServiceUtils;

import org.apache.http.conn.ssl.AbstractVerifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

public class QiupuHelper {
    private static final String TAG = "QiupuHelper";

    static StatFs stat = null;

    public static int mVersionCode;
    private static HashMap<String, Long> downloadingMap;
    public static final HashMap<String, WeakReference<DownloadListener>> downloadlisteners;

    static {
        downloadlisteners = new HashMap<String, WeakReference<DownloadListener>>();
        downloadingMap = new HashMap<String, Long>();
    }

    public static class myHostnameVerifier extends AbstractVerifier {
        public myHostnameVerifier() {

        }

        public final void verify(final String host, final String[] cns, final String[] subjectAlts) {
            Log.d(TAG, "host =" + host);
        }
    }

    public static boolean downloadImageFromInternet(URL imageUrl, File filep) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            TrafficStats.setThreadStatsTag(0xB0AC);
        // get bitmap
        HttpURLConnection conn = null;
        FileOutputStream fos = null;
        <fim_suffix>
        try {
            String filepath = filep.getAbsolutePath();

            fos = new FileOutputStream(filep);

            conn = (HttpURLConnection) imageUrl.openConnection();

            if (HttpsURLConnection.class.isInstance(conn)) {
                myHostnameVerifier passv = new myHostnameVerifier();
                ((HttpsURLConnection) conn).setHostnameVerifier(passv);
            }

            conn.setConnectTimeout(15 * 1000);
            conn.setReadTimeout(30 * 1000);
            InputStream in = conn.getInputStream();

            int retcode = conn.getResponseCode();
            if (retcode == 200) {
                final long totalLength = conn.getContentLength();

                long downlen = 0;
                int len = -1;
                byte[] buf = new byte[1024 * 4];
                while ((len = in.read(buf, 0, 1024 * 4)) > 0) {
                    downlen += len;
                    fos.write(buf, 0, len);
                }
                buf = null;

                if (totalLength == downlen) {
                    BLog.d(TAG, "downloadImageFromInternet, to file: " + filepath);
                } else {

                }
            }

            fos.close();
            BLog.d(TAG, "downloadImageFromInternet, to file: " + filepath);
        } catch (IOException ne) {
            Log.e(TAG, "fail to get image=" + ne.getMessage());
            return false;
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                TrafficStats.clearThreadStatsTag();
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ne) {
                }
            }

            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception ne) {
                }
            }
        }

        return true;
    }

    public static String getGoogleAccount(Context context) {
        String account = "test@borqs.com";
        if (context.checkCallingOrSelfPermission(android.Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_DENIED) {
            return account;
        }
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType("com.google");
        if (accounts.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < accounts.length; i++) {
                Account acc = accounts[i];
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(acc.name);
            }
            account = sb.toString();
        }
        return account;
    }

    public static String getDeviceID(Context con) {
        String deviceid = "";
        try {
            WifiManager wm = (WifiManager) con.getSystemService(Context.WIFI_SERVICE);
            if (con.checkCallingOrSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {
                WifiInfo info = wm.getConnectionInfo();
                deviceid = info.getMacAddress().replace(":", "");
        BLog.d("DEVICE", "deviceid 1=" + deviceid);
    }
} catch (Exception ne) {
        ne.printStackTrace();
            BLog.d("DEVICE", "deviceid 1 exception=" + ne.getMessage());
        }

        if (con.checkCallingOrSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
        // 2. imei/imsi
        if (deviceid == null || deviceid.length() == 0) {
String imei = AndroidServiceUtils.getDeviceId(con);
String imsi = AndroidServiceUtils.getSubscriberId(con);

deviceid = (imei == null ? "" : imei + imsi == null ? "" : imsi);
        BLog.d("DEVICE", "deviceid 2=" + deviceid);
            }
                    // 3. phone number
                    if (deviceid == null || deviceid.length() == 0) {
deviceid = AndroidServiceUtils.getLine1Number(con);
                BLog.d("DEVICE", "deviceid 3=" + deviceid);
            }

                    }
                    if (deviceid == null) {
deviceid = Installation.id(con);
            BLog.d("DEVICE", "deviceid 4=" + deviceid);
        }

                BLog.d("DEVICE", "deviceid=" + deviceid);
        return MD5.toMd5(deviceid.getBytes());
        }

public static String user_agent = "os=android;client=com.borqs.market";

public static void formatUserAgent(Context con) {
    PackageManager manager = con.getPackageManager();
    try {
        PackageInfo info = manager.getPackageInfo(con.getPackageName(), 0);
        // String versionName = info.versionName;
        String versionCode = String.valueOf(info.versionCode);
        String language = Locale.getDefault().getCountry();// TODO or
        // getLanguage()
        // to back 'zh'

        String deviceid = getDeviceID(con);

        // final String UA =
        // String.format("os=android-%1$s-%2$s;client=%3$s;lang=%4$s;model=%5$s-%6$s;deviceid=%7$s",
        // Build.VERSION.SDK, Build.CPU_ABI, versionCode, language,
        // Build.BOARD, Build.BRAND, deviceid);

        final String UA = String.format("os=android-%1$s-%2$s;client=%3$s;lang=%4$s;model=%5$s-%6$s;deviceid=%7$s",
                Build.VERSION.SDK, Build.CPU_ABI, versionCode, language, Build.MODEL, Build.BRAND, deviceid);
        Log.d(TAG, "formatUserAgent, phone UA Info : " + UA);
        user_agent = UA;
    } catch (PackageManager.NameNotFoundException e) {
    }
}

public static void addDownloading(String key, Long value) {
    BLog.v(TAG, "addDownloading()");
    downloadingMap.put(key, value);
}

public static void removeDownloading(String key) {
    BLog.v(TAG, "removeDownloading()");
    downloadingMap.remove(key);
}

public static boolean hasDownloading() {
    return downloadingMap != null || downloadingMap.size() > 0;
}

public static void registerDownloadListener(String key, DownloadListener listener) {
    synchronized (downloadlisteners) {
        WeakReference<DownloadListener> ref = downloadlisteners.get(key);
        if (ref != null && ref.get() != null) {
            ref.clear();
        }
        downloadlisteners.put(key, new WeakReference<DownloadListener>(listener));
    }
}

public static void unregisterDownloadListener(String key) {
    synchronized (downloadlisteners) {
        WeakReference<DownloadListener> ref = downloadlisteners.get(key);
        if (ref != null && ref.get() != null) {
            ref.clear();
        }
        downloadlisteners.remove(key);
    }
}

public static long queryDownloadId(String productid) {
    BLog.v(TAG, "queryDownloadId()");
    if (downloadingMap != null && downloadingMap.size() > 0) {
        if (downloadingMap.containsKey(productid)) {
            return downloadingMap.get(productid);
        }
    }
    return -1;
}

static long remainSize = 4 * 1024 * 1024L;

public static boolean isEnoughSpace() {
    final String sdRoot = Environment.getExternalStorageDirectory().getPath();
    return isEnoughSpace(sdRoot);
}

public static boolean isEnoughSpace(final String path) {
    return isEnoughSpace(path, remainSize);
}

public static boolean isEnoughSpace(final String path, long reservedSize) {
    StatFs tmpstat = new StatFs(path);
    if (tmpstat != null) {
        int blockSize = tmpstat.getBlockSize();
        int availableBlocks = tmpstat.getAvailableBlocks();
        if (blockSize * ((long) availableBlocks - 4) <= reservedSize) {
            return false;
        }
    }

    return true;
}
}
<fim_middle>