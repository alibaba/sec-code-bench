<filename>RareRouter-main/ModuleA/src/main/java/com/lxf/ModuleA/downJar/OkHttpDownload.java<fim_prefix>

        package com.lxf.ModuleA.downJar;


import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpDownload {
    public static void downLoadFile(final HttpParameter parameter, final StatusListener listener) {
        <fim_suffix>
        Request.Builder builder = new Request.Builder()
                .get()
                .url(parameter.url)
                .addHeader("Accept-Encoding", "identity")
                .addHeader("User-Agent", "okhttp/3.10.0[AWV/v1.0;AD/Android]");
        final Request request = builder.build();

        Call call = client.newCall(request);
        listener.onStart();

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                listener.onFail(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String fileName = parameter.getDownLoadFileName();
                    if (fileName == null || fileName.length() == 0) {
                        fileName = getHttpResponseFileName(response);
                    }

                    File pathFile = new File(parameter.downLoadDir);
                    File file = new File(pathFile, fileName);
                    WriteFile.writeFileFromIS(file, response.body().byteStream(), response.body().contentLength(), listener);
                } else {
                    listener.onFail("后台response code 错误");
                }

            }
        });

    }

    private static String getHttpResponseFileName(Response response) {
        String nameSentence = response.headers().get("Content-Disposition");
        if (nameSentence != null && nameSentence.contains("filename")) {
            String[] valStr = nameSentence.split(";");
            for (String str : valStr) {
                if (str.contains("filename")) {
                    String str2 = str.substring(str.indexOf("=") + 1);
                    if (str2 != null && str2.length() > 0) {
                        if (str2.startsWith("UTF-8''")) {
                            String str3 = str2.replace("UTF-8''", "");
                            try {
                                String str4 = URLDecoder.decode(str3, "UTF-8");
                                return str4;
                            } catch (Exception e) {
                                return str3;
                            }
                        } else {
                            return str2;
                        }
                    }
                    break;
                }
            }
        }
        return System.currentTimeMillis() + "";
                        }
                    }
<fim_middle>