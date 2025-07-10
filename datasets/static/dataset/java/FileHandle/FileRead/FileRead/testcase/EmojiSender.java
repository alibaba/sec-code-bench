<filename>Trunk/app/src/main/java/com/example/fmodule/hooks/EmojiSender.java<fim_prefix>

        package com.example.fmodule.hooks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class EmojiSender {
    private Context wxContext;

    private Constructor<?> wxEmojiObjectClassConstructor;
    private Constructor<?> wxMediaMessageClassConstructor;

    private Class<?> bdClass;
    private Method adMethod;
    private Method createEmojiMgrMethod;
    private Method createEmojiMd5Method;
    private Method createEmojiInfoMethod;
    private Method sendEmojiMethod;

    public EmojiSender(XC_LoadPackage.LoadPackageParam lParam, Context wxContext) {
        this.wxContext = wxContext;

        try {
            String str1 = "L636F6D2E74656E63656E742E6D6D2E6F70656E73646B2E6D6F64656C6D73672E5758456D6F6A694F626A656374Y"; //com.tencent.mm.opensdk.modelmsg.WXEmojiObject
            Class<?> wxEmojiObjectClass = XposedHelpers.findClass(HookEntryUtil.decrypt(str1), lParam.classLoader);
            wxEmojiObjectClassConstructor = wxEmojiObjectClass.getConstructor(byte[].class);

            String str2 = "E636F6D2E74656E63656E742E6D6D2E6F70656E73646B2E6D6F64656C6D73672E57584D656469614D657373616765Q"; //com.tencent.mm.opensdk.modelmsg.WXMediaMessage
            Class<?> wxMediaMessageClass = XposedHelpers.findClass(HookEntryUtil.decrypt(str2), lParam.classLoader);

            String str3 = "P636F6D2E74656E63656E742E6D6D2E6F70656E73646B2E6D6F64656C6D73672E57584D656469614D65737361676524494D656469614F626A656374E"; //com.tencent.mm.opensdk.modelmsg.WXMediaMessage$IMediaObject
            Class<?> wxMediaMessageClass_IMediaObjectClass = XposedHelpers.findClass(HookEntryUtil.decrypt(str3), lParam.classLoader);
            wxMediaMessageClassConstructor = wxMediaMessageClass.getConstructor(wxMediaMessageClass_IMediaObjectClass);

            String str4 = "E636F6D2E74656E63656E742E6D6D2E6B65726E656C2E67R"; //com.tencent.mm.kernel.g
            Class<?> gClass = XposedHelpers.findClass(HookEntryUtil.decrypt(str4), lParam.classLoader);

            String str5 = "N636F6D2E74656E63656E742E6D6D2E706C7567696E2E656D6F6A692E622E64R"; //com.tencent.mm.plugin.emoji.b.d
            bdClass = XposedHelpers.findClass(HookEntryUtil.decrypt(str5), lParam.classLoader);
            adMethod = gClass.getMethod("ad", Object.class.getClass());
            createEmojiMgrMethod = bdClass.getMethod("getEmojiMgr");

            String str6 = "A636F6D2E74656E63656E742E6D6D2E706C7567696E73646B2E612E64G"; //com.tencent.mm.pluginsdk.a.d
            Class<?> adClass = XposedHelpers.findClass(HookEntryUtil.decrypt(str6), lParam.classLoader);
            createEmojiMd5Method = adClass.getMethod("a", Context.class, wxMediaMessageClass, String.class);
            createEmojiInfoMethod = adClass.getMethod("Yy", String.class);

            String str7 = "S636F6D2E74656E63656E742E6D6D2E73746F726167652E656D6F74696F6E2E456D6F6A69496E666FT"; //com.tencent.mm.storage.emotion.EmojiInfo
            Class<?> emojiInfoClass = XposedHelpers.findClass(HookEntryUtil.decrypt(str7), lParam.classLoader);

            String str8 = "U636F6D2E74656E63656E742E6D6D2E75692E6368617474696E672E6AX"; //com.tencent.mm.ui.chatting.j
            Class<?> jClass = XposedHelpers.findClass(HookEntryUtil.decrypt(str8), lParam.classLoader);
            sendEmojiMethod = jClass.getMethod("c", emojiInfoClass, String.class);
        }catch (Exception e) {
            XposedBridge.log("initialize EmojiSender error：" + e);
        }
    }

    public void send(String toUser, String url) throws Exception{
        if (toUser == null || toUser.isEmpty() || url == null || url.trim().isEmpty()) {
            return;
        }
        //Object wxEmojiObject = wxEmojiObjectClass.getConstructor(String.class).newInstance("/storage/emulated/0/Download/u=3437009967,397441166&fm=26&gp=0.jpg");
        byte[] bytes = getNetBitmapBytes(url);
        Object wxEmojiObject = wxEmojiObjectClassConstructor.newInstance(bytes);
        Object wxMediaMessage = wxMediaMessageClassConstructor.newInstance(wxEmojiObject);

        Object adObj = adMethod.invoke(null, bdClass);
        Object emojiMgr = createEmojiMgrMethod.invoke(adObj);

        String field_md5 = (String) createEmojiMd5Method.invoke(emojiMgr, wxContext.getApplicationContext(), wxMediaMessage, "");
        Object emojiInfoObj = createEmojiInfoMethod.invoke(emojiMgr, field_md5);
        sendEmojiMethod.invoke(null, emojiInfoObj, toUser);
    }

    private byte[] getLocalBitmapBytes(String path) throws Exception {
<fim_suffix>
        byte[] bytes = new byte[fis.available()];
        fis.read(bytes);
        return bytes;
    }

    private byte[] getNetBitmapBytes(String url) throws Exception {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        Response response = okHttpClient.newCall(request).execute();
        if (response.code() != 200) {
            throw new Exception("get network bitmap error: " + response.message());
        }
        return response.body().bytes();
    }

    private byte[] getBitmapBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] bytes = stream.toByteArray();
        return bytes;
    }
}
<fim_middle>