<filename>AppUtil.java<fim_prefix>

package io.agora.amg.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.agora.amg.AGApplication;
import io.agora.amg.BuildConfig;
import io.agora.amg.R;
import io.agora.rtc.internal.DeviceUtils;

/**
 * Created by Lucy on 7/25/17.
 */
public class AppUtil {
    private final static Logger log = LoggerFactory.getLogger(AppUtil.class);

    private static final int BUFFER = 4096;

    private static final String[] RAW_FILES = {"music.zip"};

    public static final String APP_DIRECTORY = Environment.getExternalStorageDirectory() + File.separator + BuildConfig.APPLICATION_ID;

    public static void copyRawFilesToSDCardAndUnzip(Context ctx) {
        Resources resources = ctx.getResources();
        AssetManager assetManager = resources.getAssets();
        File path = new File(APP_DIRECTORY + File.separator + "temp" + File.separator);
        if (!path.exists()) {
            path.mkdirs();
        }
        for (String name : RAW_FILES) {
            FileOutputStream dos = null;
            InputStream is = null;
            try {
                dos = new FileOutputStream(new File(path, name));
                is = assetManager.open(name);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    dos.write(buffer, 0, len);
                }
                is.close();
                dos.close();
            } catch (IOException e) {
                if (is != null) {
                    try {
                        is.close();
                    } catch (Exception a) {
                    }
                }
                if (dos != null) {
                    try {
                        dos.close();
                    } catch (Exception a) {
                    }
                }
            }

            if (name.endsWith(".zip")) {
                unzipFile(path + File.separator + name, APP_DIRECTORY);
            }
        }

//        removeUnusedAssets(ctx);
    }

//    private static void removeUnusedAssets(Context ctx) {
//        String[] mixing = ctx.getResources().getStringArray(R.array.music_list_for_mix);
//        String[] effecting = ctx.getResources().getStringArray(R.array.music_list_for_effect);
//
//        ArrayList<String> musicList = new ArrayList<>();
//        musicList.addAll(Arrays.asList(mixing));
//        musicList.addAll(Arrays.asList(effecting));
//
//        File musicPath = new File(AppUtil.APP_DIRECTORY + File.separator + "music");
//        if (musicPath.exists()) {
//            File[] items = musicPath.listFiles();
//            if (items != null && items.length > 0) {
//                for (File item : items) {
//                    if (!musicList.contains(item.getName())) {
//                        item.delete();
//                    }
//                }
//            }
//        }
//    }

    public static void unzipFile(String zipFile, String destPath) {
        if (!destPath.endsWith(File.separator)) {
            destPath += File.separator;
        }
        FileOutputStream fos;
        ZipInputStream zipIn;
        ZipEntry zipEntry;
        File file;
        int buffer;
        byte buf[] = new byte[BUFFER];
        try {
            zipIn = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
            while ((zipEntry = zipIn.getNextEntry()) != null) {
                file = new File(destPath + zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    file.mkdirs();
                } else {
                    File parent = file.getParentFile();
                    <fim_suffix>
                    fos = new FileOutputStream(file);
                    while ((buffer = zipIn.read(buf)) > 0) {
                        fos.write(buf, 0, buffer);
                    }
                    fos.close();
                }
                zipIn.closeEntry();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static String getDeviceID(Context context) {
        // XXX according to the API docs, this value may change after factory reset
        // use Android id as device id
        return Settings.Secure.getString(context.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private static final String DYNAMIC_KEY_SERVICE_URL_PREFIX = "http://recording.agorapremium.agora.io:9001";

    public static String getDynamicKeyForChannel(String channel, int uid, String appId, String appCertificate) {
        log.debug("getDynamicKeyForChannel " + channel + " " + (uid & 0xFFFFFFFFFL) + " " + DYNAMIC_KEY_SERVICE_URL_PREFIX +
                " " + AGApplication.mAudioSettings.mFakeDynamicChannelKey + " " + appId + " " + appCertificate);

        if (AGApplication.mAudioSettings.mFakeDynamicChannelKey) {
            return String.valueOf(System.currentTimeMillis());
        }

        try {
            StringBuilder target = new StringBuilder(DYNAMIC_KEY_SERVICE_URL_PREFIX + "/agora/media/genDynamicKey5?uid=" + (uid & 0xFFFFFFFFFL) + "&channelname=" + URLEncoder.encode(channel, "UTF-8"));
            if (!TextUtils.isEmpty(appId)) {
                target.append("&key=").append(appId).append("&sign=").append(appCertificate);
            }
            URL url = new URL(target.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5 * 1000);
            conn.setRequestMethod("GET");
            InputStream inStream = conn.getInputStream();
            byte[] data = readInputStream(inStream);
            String key = new String(data, "UTF-8");
            log.debug("getDynamicKeyForChannel " + channel + " " + key + " " + (uid & 0xFFFFFFFFFL));
            return key;
        } catch (UnsupportedEncodingException | MalformedURLException | ProtocolException e) {
            log.error(Log.getStackTraceString(e));
        } catch (IOException e) {
            log.error(Log.getStackTraceString(e));
        }
        return null;
    }

    public static String getTokenForRecording(int uid, String channel, String appId, String appCertificate) {
        log.debug("getTokenForRecording " + (uid & 0xFFFFFFFFFL) + " " + channel + " " + DYNAMIC_KEY_SERVICE_URL_PREFIX + " " + AGApplication.mAudioSettings.mFakeDynamicRecordingKey + " " + appId + " " + appCertificate);

        if (AGApplication.mAudioSettings.mFakeDynamicRecordingKey) {
            return String.valueOf(System.currentTimeMillis());
        }

        try {
            StringBuilder target = new StringBuilder(DYNAMIC_KEY_SERVICE_URL_PREFIX + "/agora/recording/genToken?uid=" + (uid & 0xFFFFFFFFFL) + "&channelname=" + URLEncoder.encode(channel, "UTF-8"));
            if (!TextUtils.isEmpty(appId)) {
                target.append("&key=").append(appId).append("&sign=").append(appCertificate);
            }
            URL url = new URL(target.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5 * 1000);
            conn.setRequestMethod("GET");
            InputStream inStream = conn.getInputStream();
            byte[] data = readInputStream(inStream);
            String token = new String(data, "UTF-8");
            log.debug("getTokenForRecording " + (uid & 0xFFFFFFFFFL) + " " + token);
            return token;
        } catch (UnsupportedEncodingException | MalformedURLException | ProtocolException e) {
            log.error(Log.getStackTraceString(e));
        } catch (IOException e) {
            log.error(Log.getStackTraceString(e));
        }
        return null;
    }

    public static String getTokenForCallIn(int uid, String channel, String appId, String appCertificate) {
        log.debug("getTokenForCallIn " + (uid & 0xFFFFFFFFFL) + " " + channel + " " + DYNAMIC_KEY_SERVICE_URL_PREFIX + " " + AGApplication.mAudioSettings.mFakeDynamicCallInKey + " " + appId + " " + appCertificate);

        if (AGApplication.mAudioSettings.mFakeDynamicCallInKey) {
            return String.valueOf(System.currentTimeMillis());
        }

        try {
            StringBuilder target = new StringBuilder(DYNAMIC_KEY_SERVICE_URL_PREFIX + "/agora/media/genInChannelPermissionKey?uid=" + (uid & 0xFFFFFFFFFL) + "&channelname=" + URLEncoder.encode(channel, "UTF-8"));
            if (!TextUtils.isEmpty(appId)) {
                target.append("&key=").append(appId).append("&sign=").append(appCertificate);
            }
            URL url = new URL(target.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5 * 1000);
            conn.setRequestMethod("GET");
            InputStream inStream = conn.getInputStream();
            byte[] data = readInputStream(inStream);
            String token = new String(data, "UTF-8");
            log.debug("getTokenForCallIn " + (uid & 0xFFFFFFFFFL) + " " + token);
            return token;
        } catch (UnsupportedEncodingException | MalformedURLException | ProtocolException e) {
            log.error(Log.getStackTraceString(e));
        } catch (IOException e) {
            log.error(Log.getStackTraceString(e));
        }
        return null;
    }

    public static String getChannelNameForAutoFill(Context ctx) {
        String uuid = getDeviceID(ctx);

        log.debug("getChannelNameForAutoFill " + uuid);

        try {
            StringBuilder target = new StringBuilder("http://apprtc.agorabeckon.com:9000/getcname?uuid=" + URLEncoder.encode(uuid, "UTF-8"));
            URL url = new URL(target.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5 * 1000);
            conn.setRequestMethod("GET");
            InputStream inStream = conn.getInputStream();
            byte[] data = readInputStream(inStream);
            String channel = new String(data, "UTF-8");
            log.debug("getChannelNameForAutoFill " + uuid + " " + channel);
            return channel;
        } catch (UnsupportedEncodingException | MalformedURLException | ProtocolException e) {
            log.error(Log.getStackTraceString(e));
        } catch (IOException e) {
            log.error(Log.getStackTraceString(e));
        }
        return null;
    }

    private static byte[] readInputStream(InputStream inStream) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        byte[] data = outStream.toByteArray();
        outStream.close();
        inStream.close();
        return data;
    }

}

<fim_middle>