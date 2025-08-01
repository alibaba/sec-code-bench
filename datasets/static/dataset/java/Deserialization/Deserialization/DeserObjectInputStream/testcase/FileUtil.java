<filename>android/IMLib/src/main/java/com/zhangwuji/im/utils/FileUtil.java<fim_prefix>

        package com.zhangwuji.im.utils;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.zhangwuji.im.config.SysConstant;

import org.apache.http.util.EncodingUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
    public static String SDCardRoot;
    public static File updateFile;

    static {
        // 获取SD卡路径
        SDCardRoot = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    }

    /**
     * 创建文件夹
     *
     * @throws IOException
     */
    public static File createFileInSDCard(String fileName, String dir) {
        File file = new File(SDCardRoot + dir + File.separator + fileName);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateFile = file;
        return file;
    }

    /**
     * 创建目录
     *
     * @param dir
     * @return
     */
    public static File creatSDDir(String dir) {
        File dirFile = new File(SDCardRoot + dir + File.separator);
        dirFile.mkdirs();
        return dirFile;
    }

    /**
     * 检测文件是否存在
     */
    public static boolean isFileExist(String fileName, String path) {
        File file = new File(SDCardRoot + path + File.separator + fileName);
        return file.exists();
    }

    public static boolean isFileExist(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    /**
     * 通过流往文件里写东西
     */
    public static File writeToSDFromInput(String path, String fileName, InputStream input) {

        File file = null;
        OutputStream output = null;
        try {
            file = createFileInSDCard(fileName, path);
            output = new FileOutputStream(file, false);
            byte buffer[] = new byte[4 * 1024];
            int temp;
            while ((temp = input.read(buffer)) != -1) {
                output.write(buffer, 0, temp);
            }
            output.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                output.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    /**
     * 把字符串写入文件
     */
    public static File writeToSDFromInput(String path, String fileName, String data) {

        File file = null;
        OutputStreamWriter outputWriter = null;
        OutputStream outputStream = null;
        try {
            creatSDDir(path);
            file = createFileInSDCard(fileName, path);
            outputStream = new FileOutputStream(file, false);
            outputWriter = new OutputStreamWriter(outputStream);
            outputWriter.write(data);
            outputWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                outputWriter.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    public static String getFromCipherConnection(String actionUrl, String content, String path) {

        try {
            File[] files = new File[1];
            files[0] = new File(path);
            // content = CipherUtil.getCipherString(content);
            String BOUNDARY = java.util.UUID.randomUUID().toString();
            String PREFIX = "--", LINEND = "\r\n";
            String MULTIPART_FROM_DATA = "multipart/form-data";
            String CHARSET = "UTF-8";
            URL uri = new URL(actionUrl);
            HttpURLConnection conn = (HttpURLConnection) uri.openConnection();
            conn.setReadTimeout(5 * 1000); // 缓存的最长时间
            conn.setDoInput(true);// 允许输入
            conn.setDoOutput(true);// 允许输出
            conn.setUseCaches(false); // 不允许使用缓存
            conn.setRequestMethod("POST");
            conn.setRequestProperty("connection", "keep-alive");
            conn.setRequestProperty("Charsert", "UTF-8");
            conn.setRequestProperty("Content-Type", MULTIPART_FROM_DATA + ";boundary=" + BOUNDARY);
            // 首先组拼文本类型的参数
            StringBuilder sb = new StringBuilder();
            sb.append(PREFIX);
            sb.append(BOUNDARY);
            sb.append(LINEND);
            sb.append("Content-Disposition: form-data; name=\"userName\"" + LINEND);// \"userName\"
            sb.append("Content-Type: text/plain; charset=" + CHARSET + LINEND);
            sb.append("Content-Transfer-Encoding: 8bit" + LINEND);
            sb.append(LINEND);
            sb.append(content);
            sb.append(LINEND);

            DataOutputStream outStream = new DataOutputStream(conn.getOutputStream());
            outStream.write(sb.toString().getBytes());
            // 发送文件数据
            if (files != null) {
                for (File file : files) {
                    StringBuilder sb1 = new StringBuilder();
                    sb1.append(PREFIX);
                    sb1.append(BOUNDARY);
                    sb1.append(LINEND);
                    sb1.append("Content-Disposition: form-data; name=\"" + file.getName()
                            + "\"; filename=\"" + file.getName() + "\"" + LINEND);
                    sb1.append("Content-Type: application/octet-stream; charset=" + CHARSET
                            + LINEND);
                    sb1.append(LINEND);
                    outStream.write(sb1.toString().getBytes());
                    try {
                        InputStream is = new FileInputStream(file);
                        byte[] buffer = new byte[1024];
                        int len = 0;
                        while ((len = is.read(buffer)) != -1) {

                            outStream.write(buffer, 0, len);
                        }
                        is.close();
                        outStream.write(LINEND.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            // 请求结束标志
            byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINEND).getBytes();
            outStream.write(end_data);
            outStream.flush();
            outStream.close();
            // 得到响应码
            int res = conn.getResponseCode();
            if (res == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        (InputStream) conn.getInputStream()));
                String line = null;
                StringBuilder result = new StringBuilder();
                while ((line = in.readLine()) != null) {
                    result.append(line);
                }
                in.close();
                conn.disconnect();// 断开连接
                return "true";
            } else {
                return "";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

    }

    public static byte[] getFileContent(String fileName) {
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(fileName);
            int length = fin.available();
            byte[] bytes = new byte[length];
            fin.read(bytes);
            return bytes;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (fin != null) {
                    fin.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param file
     * @Description 删除文件或文件夹
     */
    public static void delete(File file) {
        if (!file.exists()) {
            return; // 不存在直接返回
        }

        if (file.isFile()) {
            file.delete(); // 若是文件则删除后返回
            return;
        }

        // 若是目录递归删除后,并最后删除目录后返回
        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();
            if (childFiles == null || childFiles.length == 0) {
                file.delete(); // 如果是空目录，直接删除
                return;
            }

            for (int i = 0; i < childFiles.length; i++) {
                delete(childFiles[i]); // 递归删除子文件或子文件夹
            }
            file.delete(); // 删除最后的空目录
        }
        return;
    }

    /**
     * @param dirFile  目录文件
     * @param timeLine 时间分隔线
     * @Description 删除目录下过旧的文件
     */
    public static void deleteHistoryFiles(File dirFile, long timeLine) {
        // 不存在或是文件直接返回
        if (!dirFile.exists() || dirFile.isFile()) {
            return;
        }

        try {
            // 如果是目录则删除过旧的文件
            if (dirFile.isDirectory()) {
                File[] childFiles = dirFile.listFiles();
                if (childFiles == null || childFiles.length == 0) {
                    return; // 如果是空目录就直接返回
                }
                // 遍历文件，删除过旧的文件
                Long fileTime;
                for (int i = 0; i < childFiles.length; i++) {
                    fileTime = childFiles[i].lastModified();
                    if (fileTime < timeLine) {
                        delete(childFiles[i]);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static File save2File(String savePath, String saveName,
                                 String crashReport) {
        try {
            File dir = new File(savePath);
            if (!dir.exists())
                dir.mkdir();
            File file = new File(dir, saveName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(crashReport.toString().getBytes());
            fos.close();
            return file;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String saveAudioResourceToFile(byte[] content, int userId) {
        try {
            String audioSavePath = CommonUtil.getAudioSavePath(userId);
            File file = new File(audioSavePath);
            FileOutputStream fops = new FileOutputStream(file);
            fops.write(content);
            fops.flush();
            fops.close();
            return audioSavePath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String saveGifResourceToFile(byte[] content) {
        try {
            String gifSavePath = CommonUtil.getSavePath(SysConstant.FILE_SAVE_TYPE_IMAGE);
            File file = new File(gifSavePath);
            FileOutputStream fops = new FileOutputStream(file);
            fops.write(content);
            fops.flush();
            fops.close();
            return gifSavePath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    @SuppressWarnings("unused")
    private static Bitmap getBitmap(InputStream fs) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = 1;
        Bitmap imgBitmap = BitmapFactory.decodeStream(fs, null, opts);
        if (imgBitmap != null) {
            int width = imgBitmap.getWidth();
            int height = imgBitmap.getHeight();
            imgBitmap = Bitmap.createScaledBitmap(imgBitmap, width, height,
                    true);
        }
        return imgBitmap;
    }

    public static long getFileLen(File file) {
        long total = 0;
        try {
            InputStream is = new FileInputStream(file);
            byte[] bytes = new byte[1024];
            int len = 0;
            while ((len = is.read(bytes)) != -1) {
                total += len;
            }
            is.close();
        } catch (Exception e) {

        }
        return total;
    }

    public static boolean isSdCardAvailuable() {

        boolean bRet = false;
        do {
            if (!Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED)) {
                break;
            }

            if (CommonUtil.getSDFreeSize() < 5) {
                break;
            }

            bRet = true;
        } while (false);

        return bRet;
    }

    public static byte[] InputStreamToByte(InputStream is) throws IOException {
        ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
        int ch;
        while ((ch = is.read()) != -1) {
            bytestream.write(ch);
        }
        byte imgdata[] = bytestream.toByteArray();
        bytestream.close();


        return imgdata;
    }

    public static byte[] File2byte(String filePath) {
        byte[] buffer = null;
        try {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }

    public static String getExtensionName(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length() - 1))) {
                return filename.substring(dot + 1);
            }
        }
        return filename;
    }


    public static String readFile(Context app, String key) {
        return readFile(app, key, 0);
    }

    // 读取文件
    public static String readFile(Context mContext, String key, int minutes) {
        try {
            File file = new File(mContext.getFilesDir(), key + ".dat");
            if (!file.exists()) {
                return "";
            }
            if (minutes > 0) {
                long s = file.lastModified();
                long n = System.currentTimeMillis();
                int c = getDistMinutes(s, n);
                if (c >= minutes) {
                    file.delete();
                    return "";
                }
            }
            FileInputStream fis = new FileInputStream(file);
            int length = fis.available();
            byte[] buffer = new byte[length];
            fis.read(buffer);
            String res = EncodingUtils.getString(buffer, "UTF-8");
            fis.close();
            return res;
        } catch (Exception ex) {
            return "";
        }

    }

    // 写入文件
    public static void writeFile(Context mContext, String key, String write_str) {
        FileOutputStream fos = null;
        try {
            fos = mContext.openFileOutput(key + ".dat", Context.MODE_PRIVATE);
            fos.write(write_str.getBytes());
        } catch (Exception ex) {
        } finally {
            try {
                fos.flush();
                fos.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static int getDistMinutes(long startDate, long endDate) {
        long diff = endDate - startDate;
        long minutes = diff / (1000 * 60);
        return (int) minutes;
    }

    /**
     * 保存对象（files目录下）
     *
     * @param ser
     * @param file
     * @throws IOException
     */
    public static boolean saveObject(Context mContext, Serializable ser,
                                     String file) {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = mContext.openFileOutput(file, mContext.MODE_PRIVATE);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(ser);
            oos.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                oos.close();
            } catch (Exception e) {
            }
            try {
                fos.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * 读取对象（files目录下）
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static Serializable readObject(Context mContext, String file) {
        if (!isExistDataCache(mContext, file))
            return null;
        FileInputStream fis = null;
        ObjectInputStream ois = null;
    try{
        <fim_suffix>
    } catch(
    Exception e)

    {
        e.printStackTrace();
        // ______澶辫触 - ___缂____欢
        if (e instanceof InvalidClassException) {
            File data = mContext.getFileStreamPath(file);
            data.delete();
        }
    } finally

    {
        try {
            ois.close();
        } catch (Exception e) {
        }
        try {
            fis.close();
        } catch (Exception e) {
        }
    }
        return null;
}

/**
 * 判断缓存是否存在（files目录）
 *
 * @param cachefile
 * @return
 */
public static boolean isExistDataCache(Context mContext, String cachefile) {
    boolean exist = false;
    File data = mContext.getFileStreamPath(cachefile);
    if (data.exists())
        exist = true;
    return exist;
}

/**
 * 读取表情配置文件
 *
 * @return
 */
public static List<String> getEmojiFile(Context context) {
    try {
        List<String> list = new ArrayList<String>();
        InputStream in = context.getResources().getAssets().open("emoji");// 表情文件
        BufferedReader br = new BufferedReader(new InputStreamReader(in,
                "UTF-8"));
        String str = null;
        while ((str = br.readLine()) != null) {
            list.add(str);
        }
        return list;
    } catch (IOException e) {
        e.printStackTrace();
    }
    return null;
}

/**
 * 根据文件绝对路径获取文件名
 *
 * @param filePath
 * @return
 */
public static String getFileName(String filePath) {
    if (isEmpty(filePath))
        return "";
    return filePath.substring(filePath.lastIndexOf(File.separator) + 1);
}

/**
 * 判断给定字符串是否空白串。 空白串是指由空格、制表符、回车符、换行符组成的字符串 若输入字符串为null或空字符串，返回true
 *
 * @param input
 * @return boolean
 */
public static boolean isEmpty(String input) {
    if (input == null || input.equals("null") || "".equals(input))
        return true;

    for (int i = 0; i < input.length(); i++) {
        char c = input.charAt(i);
        if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
            return false;
        }
    }
    return true;
}
}
<fim_middle>


