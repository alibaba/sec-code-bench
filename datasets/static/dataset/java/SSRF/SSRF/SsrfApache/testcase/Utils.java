<filename>AntiCatAnticheat-master/shared/src/main/java/com/alphaautoleak/utils/Utils.java<fim_prefix>

        package com.alphaautoleak.utils;

import com.alphaautoleak.AntiCatAntiCheat;
import com.alphaautoleak.config.ConfigManager;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @Author: SnowFlake
 * @Date: 2022/6/4 00:23
 */
public class Utils {

    public static long getLongRandom(int len) {
        String str = "1234567890";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            int num = random.nextInt(10);
            sb.append(str.charAt(num));
        }
        return Long.parseLong(sb.toString());
    }

    public static String md5(String main) {
        StringBuilder sb = new StringBuilder();
        try {
            byte[] bytes = main.getBytes(StandardCharsets.UTF_8);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5 = md.digest(bytes);
            int i = 0;
            for (byte b : md5) {
                sb.append(Integer.toHexString((b & 0xFF) | 0x300), 0, 3);
                if (i != md5.length - 1) {
                    sb.append("");
                }
                i++;
            }
        } catch (Exception ignored) {
        }
        return sb.toString();
    }


    public static String getHWID() {
        StringBuilder sb = new StringBuilder();
        String main = System.getenv("COMPUTERNAME") +
                System.getProperty("os.name") +
                System.getProperty("os.version") +
                System.getProperty("os.arch") +
                Runtime.getRuntime().availableProcessors();
        try {
            byte[] bytes = main.getBytes(StandardCharsets.UTF_8);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5 = md.digest(bytes);
            int i = 0;
            for (byte b : md5) {
                sb.append(Integer.toHexString((b & 0xFF) | 0x300), 0, 3);
                if (i != md5.length - 1) {
                    sb.append("");
                }
                i++;
            }
        } catch (Exception ignored) {

        }
        return sb.toString();
    }

    public static void debug(String log){
        String path = "C:\\Users\\Administrator\\Desktop\\debug.txt";
        Utils.writeDebug(Utils.read(path)+log+"\n",path);
    }

    public static String doGet(String string){
        <fim_suffix>
        try {
            InputStream inputStream = new URL(string).openStream();
            StringWriter writer = new StringWriter();
            IOUtils.copy(inputStream,writer, StandardCharsets.UTF_8.name());
            return writer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }


    public static BufferedImage getCustomImage() {
        try {
            Random random = new Random();
            File[] files = ConfigManager.image.listFiles();

            if (files.length != 0) {
                File file = files[random.nextInt(files.length)];

                return ImageIO.read(new FileInputStream(file));
            }else{
                JOptionPane.showConfirmDialog(null,"你放点图片可以吗");
            }
        }catch (Exception e)
        {

        }
        return null;
    }

    public static void writeDebug(String content, String path) {
        File file = new File(path);
        try {
            if (!file.exists()) {
                return;
            }
            PrintWriter printwriter = new PrintWriter(file);
            printwriter.write(content);
            printwriter.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void write(String content, String path) {
        File file = new File(path);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            PrintWriter printwriter = new PrintWriter(file);
            printwriter.write(content);
            printwriter.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String read(String path) {
        File file = new File(path);
        if (file.exists()) {
            try {
                BufferedReader bufferedreader = new BufferedReader(new FileReader(file));
                String content = "";
        String line;
        while ((line = bufferedreader.readLine()) != null) {
            if (line.length() > 0) {
                content += line + "\n";
            }
        }
        bufferedreader.close();
        return content;
    } catch (Exception e) {
        e.printStackTrace();
    }
}
        return "null";
                }
                }
<fim_middle>