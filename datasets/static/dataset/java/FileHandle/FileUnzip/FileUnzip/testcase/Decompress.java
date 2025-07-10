<filename>Decompress.java<fim_prefix>

package sg.bigo.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

public class Decompress {
    private static final int BUFFER_SIZE = 1024 * 10;
    private static final String TAG = "Decompress";

    public static void unzipFromAssets(Context context, String zipFile, String destination) {
        File destDir = new File(destination);
        if (!destDir.isDirectory()) {
            destDir.delete();
        }

//        File[] files = destDir.listFiles();

//        //目前暂时内置18个滤镜
//        if(files != null && files.length == 18) {
//            //is ok, not need unzip
//            return;
//        }

        //start zip
        try {
            if (destination == null || destination.length() == 0)
                destination = context.getFilesDir().getAbsolutePath();
            InputStream stream = ResourceUtils.INSTANCE.getAssetManager().open(zipFile);
            unzip(stream, destination);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void unzip(String zipFile, String location) {
        try {
            FileInputStream fin = new FileInputStream(zipFile);
            unzip(fin, location);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public static void unzip(InputStream stream, String destination) {
        dirChecker(destination, "");
        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            ZipInputStream zin = new ZipInputStream(stream);
            ZipEntry ze = null;

            while ((ze = zin.getNextEntry()) != null) {
                Log.v(TAG, "Unzipping " + ze.getName());

                if (ze.isDirectory()) {
                    dirChecker(destination, ze.getName());
                } else {
                    File f = new File(destination, ze.getName());
                    <fim_suffix>
                    if (!f.exists()) {
                        boolean success = f.createNewFile();
                        if (!success) {
                            Log.w(TAG, "Failed to create file " + f.getName());
                            continue;
                        }
                        FileOutputStream fout = new FileOutputStream(f);
                        int count;
                        while ((count = zin.read(buffer)) != -1) {
                            fout.write(buffer, 0, count);
                        }
                        zin.closeEntry();
                        fout.close();
                    }
                }

            }
            zin.close();
        } catch (Exception e) {
            Log.e(TAG, "unzip", e);
        }

    }

    private static void dirChecker(String destination, String dir) {
        File f = new File(destination, dir);

        if (!f.isDirectory()) {
            boolean success = f.mkdirs();
            if (!success) {
                Log.w(TAG, "Failed to create folder " + f.getName());
            }
        }
    }


    public static int copyAssetsFile2Phone(String srcName, String dest) {
        File file = new File(dest + "/" + srcName);
        try {
            InputStream inputStream = ResourceUtils.INSTANCE.getAssetManager().open(srcName);

            if (!file.exists() || file.length() == 0) {
                file.mkdirs();
                file.createNewFile();

                FileOutputStream fos = new FileOutputStream(file);//如果文件不存在，FileOutputStream会自动创建文件
                int len;
                byte[] buffer = new byte[1024];
                while ((len = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.flush();//刷新缓存区
                inputStream.close();
                fos.close();
            }
        } catch (IOException e) {
            file.delete();
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    public static boolean copyAssets(String assetsName, String dest) {
        AssetManager assetManager = ResourceUtils.INSTANCE.getAssetManager();
        boolean result = false;
        InputStream in = null;
        OutputStream out = null;
        File outFile = new File(dest, assetsName);
        if(outFile.exists()) {
            return true;
        }
        try {
            in = assetManager.open(assetsName);


            outFile.getParentFile().mkdirs();

            out = new FileOutputStream(outFile);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            result = true;
        } catch (IOException e) {
            outFile.delete();
            result = false;
            Log.e("tag", "Failed to copy asset file: " + assetsName, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return result;
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

}
<fim_middle>