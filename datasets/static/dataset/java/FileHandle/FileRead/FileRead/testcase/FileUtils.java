<filename>gt-DataBinding/src/main/java/com/gsls/gt_databinding/utils/FileUtils.java<fim_prefix>

        package com.gsls.gt_databinding.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileUtils {

    public static void saveData(String saveData, String savePaht, String fileName) {
        save(saveData, savePaht, fileName);
    }

    public static void saveDataAll(String savePaht, Map<String, String> saveMap) {
        for (String key : saveMap.keySet()) {
            save(saveMap.get(key), savePaht, key);
        }
    }

    public static boolean fileExist(String filePathAddFileName) {
        return new File(filePathAddFileName).exists();
    }

    /**
     * 该文件是否为文件夹
     * @param filePath
     * @return
     */
    public static boolean fileIsDirectory(String filePath) {
        File file = new File(filePath);
        boolean isFolder = false;
        if (file.exists()) {
            isFolder = file.isDirectory();
        }
        return isFolder;
    }

    public static void save(String saveData, String savePaht, String fileName) {
        File fileNull = new File(savePaht);
        if (!fileNull.exists()) {
            fileNull.mkdirs();
        }

        File file = new File(savePaht, fileName);
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(file);
            fos.write(saveData.getBytes());
            fos.flush();
        } catch (FileNotFoundException var18) {
            var18.printStackTrace();
        } catch (IOException var19) {
            var19.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException var17) {
                    var17.printStackTrace();
                }
            }

        }

    }

    public static String query(String filePath) {
        if(!FileUtils.fileExist(filePath)) return "";
        int lastIndexOf = filePath.lastIndexOf("\\") + 1;
        String queryPath = filePath.substring(0, lastIndexOf);
        String fileName = filePath.substring(lastIndexOf);

        File fileNull = new File(queryPath);
        if (!fileNull.exists()) {
            fileNull.mkdirs();
        }

        File file = new File(queryPath, fileName);
        FileInputStream fis = null;
        byte[] buffer = null;
        String data = null;

        try {
<fim_suffix>
            buffer = new byte[fis.available()];
            fis.read(buffer);
        } catch (FileNotFoundException var19) {
            var19.printStackTrace();
        } catch (IOException var20) {
            var20.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                    data = new String(buffer);
                } catch (IOException var18) {
                    var18.printStackTrace();
                }
            }

        }

        return data;
    }

    public static String query(String queryPath, String fileName) {
        File fileNull = new File(queryPath);
        if (!fileNull.exists()) {
            fileNull.mkdirs();
        }

        File file = new File(queryPath, fileName);
        FileInputStream fis = null;
        byte[] buffer = null;
        String data = null;

        try {
            fis = new FileInputStream(file);
            buffer = new byte[fis.available()];
            fis.read(buffer);
        } catch (FileNotFoundException var19) {
            var19.printStackTrace();
        } catch (IOException var20) {
            var20.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                    data = new String(buffer);
                } catch (IOException var18) {
                    var18.printStackTrace();
                }
            }

        }

        return data;
    }

    public static List<String> queryFilePathFileNumber(String filePath) {

        List<String> fileNameList = new ArrayList<>();

        File file = new File(filePath);
        if (!file.exists()) {
            return fileNameList;
        }

        try {
            File[] files = file.listFiles();
            if (files == null || files.length == 0) return fileNameList;
            for (File fileOne : files) {
                if (fileOne.isDirectory()) {
                    fileNameList.add("目录:" + fileOne.getName());
                } else {
                    fileNameList.add("文件:" + fileOne.getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return fileNameList;
        }
        return fileNameList;
    }

    public static String getFileSizeDescriptor(String filePath, String fileName) {

        try {
            long fileSize = getFileSize(new File(filePath, fileName));
            String fileDescriptor = formetFileSize(fileSize);
            return fileDescriptor;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static long getFileSize(File file) throws Exception {
        long size = 0;
        if (file.exists()) {
            FileInputStream fis = null;
            fis = new FileInputStream(file);
            size = fis.available();
        } else {
            file.createNewFile();
        }
        return size;
    }

    private static String formetFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize = "0B";
        if (fileS == 0) {
            return wrongSize;
        }
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + "KB";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "MB";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + "GB";
        }
        return fileSizeString;
    }

    public static void deleteAllFiles(File deleteFile, boolean deleteThisFile) {

        if (!deleteFile.exists()) {
            return;
        }

        File files[] = deleteFile.listFiles();
        if (files != null && files.length != 0) {
            for (File file : files) {
                if (file.isDirectory()) { // 判断是否为文件夹
                    deleteAllFiles(file, deleteThisFile);
                    try {
                        file.delete();
                    } catch (Exception e) {
                    }
                } else {
                    if (file.exists()) { // 判断是否存在
                        deleteAllFiles(file, deleteThisFile);
                        try {
                            file.delete();
                        } catch (Exception e) {
                        }
                    }
                }
            }

            //是否删除当前文件
            if (deleteThisFile) {
                try {
                    deleteFile.delete();
                } catch (Exception e) {
                }
            }

        } else {
            try {
                deleteFile.delete();
            } catch (Exception e) {
            }
        }
    }

    public static void changeFileDirName(String path, String oldName, String newName) {
        File oldFile = new File(path + "/" + oldName);
        File newFile = new File(path + "/" + newName);

        oldFile.renameTo(newFile);
    }

    public static List<String> getUsbPath() {
        List<String> usbData = getUsbData();
        if (usbData == null || usbData.size() == 0) return null;
        List<String> usbPath = new ArrayList<>();
        for (String userData : usbData) {
            userData = userData.substring(userData.lastIndexOf("/") + 1);
            usbPath.add("/storage/" + userData + "/");
        }
        return usbPath;
    }

    public static List<String> getUsbName() {
        List<String> usbData = getUsbData();
        if (usbData == null || usbData.size() == 0) return null;
        List<String> usbName = new ArrayList<>();
        for (String userData : usbData) {
            userData = userData.substring(userData.lastIndexOf("/") + 1);
            usbName.add(userData);
        }
        return usbName;
    }

    public static List<String> getUsbData() {
        String filePath = "/proc/mounts";
        File file = new File(filePath);
        List<String> lineList = new ArrayList<>();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "GBK");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line = "";
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.contains("vfat")) {
                        lineList.add(line);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (lineList.size() == 0) return null;
        List<String> usbPaths = new ArrayList<>();
        for (int i = 0; i < lineList.size(); i++) {
            String editPath = lineList.get(i);
            int start = editPath.indexOf("/mnt");
            int end = editPath.indexOf(" vfat");
            usbPaths.add(editPath.substring(start, end));
        }
        return usbPaths;
    }

    public static void copyFile(String fromFileStr, String toFileStr) {

        File fromFile = new File(fromFileStr);
        File toFile = new File(toFileStr);

        if (!fromFile.exists()) {
            return;
        }

        copy(fromFileStr, toFileStr);
    }

    private static int copy(String fromFile, String toFile) {
        try {
            InputStream inputStream = new FileInputStream(fromFile);
            OutputStream outputStream = new FileOutputStream(toFile);
            byte bt[] = new byte[1024];
            int d;
            while ((d = inputStream.read(bt)) > 0) {
                outputStream.write(bt, 0, d);
            }
            inputStream.close();
            outputStream.close();
            return 0;
        } catch (Exception e) {
            return -1;
        }
    }

    public static int copyAllFile(String fromFile, String toFile) {
        File[] currentFiles;
        File root = new File(fromFile);
        if (!root.exists()) {
            return -1;
        }
        currentFiles = root.listFiles();
        if (currentFiles == null) {
            FileUtils.copyFile(fromFile, toFile);
            return 0;
        }

        File targetDir = new File(toFile);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        for (int i = 0; i < currentFiles.length; i++) {
            if (currentFiles[i].isDirectory()) {
//                GT.log("1 " + currentFiles[i].getPath() + "/" + "--------" + toFile + "/" + currentFiles[i].getName() + "/");
                copyAllFile(currentFiles[i].getPath() + "/", toFile + "/" + currentFiles[i].getName() + "/");

            } else {
//                GT.log("2 " + currentFiles[i].getPath() + "--------" + toFile + "/" + currentFiles[i].getName());
                CopySdcardFile(currentFiles[i].getPath(), toFile + "/" + currentFiles[i].getName());
            }
        }
        return 0;
    }

    private static int CopySdcardFile(String fromFile, String toFile) {
        try {
            InputStream fosfrom = new FileInputStream(fromFile);
            OutputStream fosto = new FileOutputStream(toFile);
            byte bt[] = new byte[1024];
            int c;
            while ((c = fosfrom.read(bt)) > 0) {
                fosto.write(bt, 0, c);
            }
            fosfrom.close();
            fosto.close();
            return 0;

        } catch (Exception ex) {
            return -1;
        }
    }

    public static List<String> getFilesAllName(String path) {
        File file = new File(path);
        File[] files = file.listFiles();
        if (files == null) {
            return null;
        }
        List<String> s = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            s.add(files[i].getAbsolutePath());
        }
        return s;
    }

    public static void deleteAllFile(File file, boolean isSaveFolder) {

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                deleteAllFile(f, isSaveFolder);
            }
            if (!isSaveFolder) {
                file.delete();
            }
        } else if (file.exists()) {
            file.delete();
        }
    }



}
<fim_middle>