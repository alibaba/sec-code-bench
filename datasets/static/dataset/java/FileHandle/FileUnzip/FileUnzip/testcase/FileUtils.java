<filename>FileUtils.java<fim_prefix>

package mplugindemo.shengyuan.com.mplugin_core.util;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by marco on 2017/9/7.
 */

public class FileUtils {

    private static final String TAG = FileUtils.class.getSimpleName();
    private static int BUFFER = 4096;


    public static String copySo(Context context, String dexPath) {
        String fileName=dexPath.substring(dexPath.lastIndexOf("/"),dexPath.length()).replace("/","");
        String unZipPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator+fileName.replace(".apk","")+File.separator;
        Log.i(TAG, "dexPath " + fileName);
        Log.i(TAG, "unZipPath " + unZipPath);
        String soPath = getSoPath(context,fileName);
        Log.i(TAG, "soPath " + soPath);
        if ((new File(soPath)).exists()) {
            Log.i(TAG, "soPath exists");
            return soPath;
        }
        try {
            copyAssetsToSdCard(context,fileName,Environment.getExternalStorageDirectory().getAbsolutePath()
                    + File.separator);
            unZip(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + File.separator+fileName, unZipPath);//解压插件apk
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<File> fileList = getFile(new File(unZipPath + "lib/"));
        Log.i(TAG, "fileList end " + fileList);
        for (int i = 0; i < fileList.size(); i++) {
            File file = fileList.get(i);
            String parentPath = file.getParentFile().getAbsolutePath();
            Log.i(TAG, "parentPath " + Build.CPU_ABI);
            if(parentPath.contains(Build.CPU_ABI)) {
                Log.i(TAG, "parentPath " + soPath);
                try {
                    copyFolder(parentPath, soPath);//copy插件的so到唯一单独文件夹
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        deleteDirWihtFile(new File(unZipPath));//删除解压包
        return soPath;
    }

    static List<File> mFileList;

    public static List<File> getFile(File file) {
        mFileList = mFileList == null ? mFileList = new ArrayList<>() : mFileList;
        File[] fileArray = file.listFiles();
        for (File f : fileArray) {
            if (f.isFile()) {
                mFileList.add(f);
            } else {
                getFile(f);
            }
        }
        return mFileList;
    }

    public static void unZip(InputStream inputStream, String targetDir){
        String strEntry; //保存每个zip的条目名称
        try {
            BufferedOutputStream dest = null; //缓冲输出流
            ZipInputStream zis = new ZipInputStream(inputStream);
            ZipEntry entry; //每个zip条目的实例
            while ((entry = zis.getNextEntry()) != null) {
                try {
                    int count;
                    byte data[] = new byte[BUFFER];
                    strEntry = entry.getName();
                    File entryFile = new File(targetDir + strEntry);
                    <fim_suffix>
                    FileOutputStream fos = new FileOutputStream(entryFile);
                    dest = new BufferedOutputStream(fos, BUFFER);
                    while ((count = zis.read(data, 0, BUFFER)) != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.flush();
                    dest.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            zis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyAssetsToSdCard(Context context, String fileName, String filePath) {
        InputStream inputStream;
        try {
            inputStream = context.getResources().getAssets().open(fileName);// assets文件夹下的文件
            File file = new File(filePath);
            if (!file.exists()) {
                file.mkdirs();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(filePath + "/" + fileName);// 保存到本地的文件夹下的文件
            byte[] buffer = new byte[BUFFER];
            int count = 0;
            while ((count = inputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, count);
            }
            fileOutputStream.flush();
            fileOutputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static void unZip(String zipFile, String targetDir) {
        String strEntry; //保存每个zip的条目名称
        try {
            BufferedOutputStream dest = null; //缓冲输出流
            FileInputStream fis = new FileInputStream(zipFile);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry; //每个zip条目的实例
            while ((entry = zis.getNextEntry()) != null) {
                try {
                    int count;
                    byte data[] = new byte[BUFFER];
                    strEntry = entry.getName();
                    File entryFile = new File(targetDir + strEntry);
                    File entryDir = new File(entryFile.getParent());
                    if (!entryDir.exists()) {
                        entryDir.mkdirs();
                    }
                    FileOutputStream fos = new FileOutputStream(entryFile);
                    dest = new BufferedOutputStream(fos, BUFFER);
                    while ((count = zis.read(data, 0, BUFFER)) != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.flush();
                    dest.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            zis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //删除文件夹和文件夹里面的文件
    public static void deleteDir(final String pPath) {
        File dir = new File(pPath);
        deleteDirWihtFile(dir);
    }

    public static void deleteDirWihtFile(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory())
            return;
        for (File file : dir.listFiles()) {
            if (file.isFile())
                file.delete(); // 删除所有文件
            else if (file.isDirectory())
                deleteDirWihtFile(file); // 递规的方式删除文件夹
        }
        dir.delete();// 删除目录本身
    }

    public static void deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 复制单个文件
     *
     * @param oldPath String 原文件路径 如：c:/fqf.txt
     * @param newPath String 复制后路径 如：f:/fqf.txt
     * @return boolean
     */
    public static void copyFile(String oldPath, String newPath) {
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(oldPath);
            File newFile = new File(newPath);
            if (!newFile.exists()) {
                newFile.createNewFile();
            }
            if (oldfile.exists()) { //文件存在时
                InputStream inStream = new FileInputStream(oldPath); //读入原文件
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                int length;
                while ((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread; //字节数 文件大小
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 复制整个文件夹内容
     *
     * @param oldPath String 原文件路径 如：c:/fqf
     * @param newPath String 复制后路径 如：f:/fqf/ff
     * @return boolean
     */
    public static void copyFolder(String oldPath, String newPath) throws IOException {
        File file = new File(oldPath);
        String[] filePath = file.list();

        if (!(new File(newPath)).exists()) {
            (new File(newPath)).mkdirs();
        } else {
            //如果存在直接返回
            return;
        }

        for (int i = 0; i < filePath.length; i++) {
            if ((new File(oldPath + file.separator + filePath[i])).isDirectory()) {
                copyFolder(oldPath + file.separator + filePath[i], newPath + file.separator + filePath[i]);
            }

            if (new File(oldPath + file.separator + filePath[i]).isFile()) {
                copyFile(oldPath + file.separator + filePath[i], newPath + file.separator + filePath[i]);
            }

        }
    }

    private static String getSoPath(Context context,String name){
        return "/data/data/" + context.getPackageName() +File.separator+ name.replace(".apk","") +File.separator+"lib"+File.separator+Build.CPU_ABI;
    }
}

<fim_middle>