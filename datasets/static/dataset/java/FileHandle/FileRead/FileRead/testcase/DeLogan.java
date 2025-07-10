<filename>DeLogan.java<fim_prefix>

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 *
 * @author foree
 * @version 1.0
 * @date 2020/7/22
 */
public class DeLogan {

    private static final char ENCRYPT_CONTENT_START = '\1';

    private static final String AES_ALGORITHM_TYPE = "AES/CBC/NoPadding";

    private static boolean VERBOSE;
    private static boolean GREEDY;

    private String key = "0123456789012345";
    private String iv = "0123456789012345";

    private ByteBuffer wrap;
    private FileOutputStream fileOutputStream;

    public static void main(String[] args){

        String fun = "help";
        if(args != null && args.length > 0) {
            fun = args[0];
        }

        // verbose mode ?
        if(args != null && args.length > 0){
            for (String arg : args) {
                if("verbose".equals(arg)){
                    VERBOSE = true;
                }
                if("greedy".equals(arg)){
                    GREEDY = true;
                }
            }
        }

        File logan = null;
        switch (fun){
            case "help":
                System.out.println("java DeLogan [help] encryptFileOrDirPath [verbose|greedy]");
                return;
            default:
                logan = new File(args[0]);
        }

        // support dir
        if(logan.isDirectory()){
            File[] files = logan.listFiles();
            for (File file : files) {
                if(file.isDirectory()){
                    continue;
                }
                if(file.getName().contains("output")){
                    System.out.println("ignore: " + file.getName());
                    continue;
                }
                parseFile(file);
            }
        }else if (logan.isFile()){
            parseFile(logan);
        }


    }

    private static void parseFile(File input){
        try {
            File output = new File(input.getAbsolutePath() + "_output");
            System.out.println("parsing: " + input.getAbsolutePath() + " to " + output.getAbsolutePath());
<fim_suffix>
            DeLogan loganProtocol = new DeLogan(fileInputStream, output);
            loganProtocol.process();
            loganProtocol.closeFileSteam();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public DeLogan(InputStream stream, File file) {
        try {
            int ch = 0;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            while((ch = stream.read()) != -1){
                outputStream.write(ch);
            }

            wrap = ByteBuffer.wrap(outputStream.toByteArray());

            outputStream.close();

            fileOutputStream = new FileOutputStream(file);
        } catch (IOException e) {
            System.out.println("init error: "  + e.getMessage());
        }
    }

    int i = 0;
    public boolean process() {
        while (wrap.hasRemaining()) {
            byte header = wrap.get();
            while (header == ENCRYPT_CONTENT_START) {
                i++;
                System.out.println("get block: " + i);

                int length = wrap.getInt();
                if(GREEDY) {
                    if (length <= 0) {
                        continue;
                    }
                }

                byte[] encrypt = new byte[length];
                if (!tryGetEncryptContent(encrypt) || !decryptAndAppendFile(encrypt)) {
                    if(!GREEDY){ // non-greedy
                        return false;
                    }
                    // continue
                }

                try {
                    header = wrap.get();
                } catch (java.nio.BufferUnderflowException e) {
                    System.out.println("tryGetEncryptContent error: " + e.getMessage());
                    return false;
                }

            }
        }
        return true;
    }

    private boolean tryGetEncryptContent(byte[] encrypt) {
        try {
            wrap.get(encrypt);
        } catch (java.nio.BufferUnderflowException e) {
            System.out.println("tryGetEncryptContent error: " + e.getMessage());
            if(GREEDY) {// greedy mode, match string more
                wrap.position(wrap.position() - 3); // 如果读出的字节数量太大，重置回 ENCRYPT_CONTENT_START 的后一个字节，尝试修复读取后边的代码块
            }

            return false;
        }
        return true;
    }

    private boolean decryptAndAppendFile(byte[] encrypt) {
        System.out.println("decryptAndAppendFile......: position: " + (wrap.position() - encrypt.length) + ", length: " + encrypt.length);
        boolean result = false;
        try {
            Cipher aesEncryptCipher = Cipher.getInstance(AES_ALGORITHM_TYPE);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "AES");
            aesEncryptCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv.getBytes()));
            byte[] compressed = aesEncryptCipher.doFinal(encrypt);
            byte[] plainText = decompress(compressed);
            if(VERBOSE) {
                System.out.println("deLogan: " + new String(plainText));
            }
            result = true;

            String output = new String(plainText);
            // do format
//            if(null != plainText && plainText.length > 0) {
//                String[] outputList = output.split("\\n");
//                for (String s : outputList) {
//                    // find "l"
//                    int start = s.indexOf("\"l\":") + 4;
//                    int end = s.substring(start).indexOf(",") + start;
//                    String timeString = s.substring(start, end);
//                    long time = Long.valueOf(timeString);
//                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS");
//                    String formatedTime = "\"" + dateFormat.format(new Date(time)) + "\"";
//                    output = output.replace(timeString, formatedTime);
//                }
//            }

            fileOutputStream.write(output.getBytes());
            fileOutputStream.flush();
        } catch (Exception e) {
            System.out.println("decryptAndAppendFile error: " + e.getMessage());
        }
        return result;
    }

    private byte[] decompress(byte[] contentBytes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            InputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(contentBytes));

            int ch = 0;
            while((ch = inputStream.read()) != -1){
                out.write(ch);
            }
            if(VERBOSE) {
                System.out.println("decompress: " + new String(out.toByteArray()));
            }
            return out.toByteArray();
        } catch (IOException e) {
            System.out.println("decompress error: " + e.getMessage());
            ByteArrayOutputStream errorOut = new ByteArrayOutputStream();
            try {
                errorOut.write(e.getMessage().getBytes());
                errorOut.write('\n');
                errorOut.write(out.toByteArray());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            return errorOut.toByteArray();

        }
    }

    public void closeFileSteam() {
        try {
            fileOutputStream.close();
        } catch (IOException e) {
            System.out.println("closeFileSteam error: " + e.getMessage());
        }
    }

}
<fim_middle>