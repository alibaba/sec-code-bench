<filename>Java网络编程源码/bio/src/main/java/cxiao/sh/cn/comm/Receiver.java<fim_prefix>

        package cxiao.sh.cn.comm;

import com.alibaba.fastjson.JSON;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.List;

/**
 * @program: Java网络编程进阶
 * @author:  Xiao Chuan
 * @email:   cxiao@fudan.edu.cn
 * @create:  2020.09
 **/

public class Receiver {
    //从socket接收字节，把数组bytes填满
    private static void readFully(Socket socket, byte[] bytes) throws Exception{
        InputStream ins = socket.getInputStream();
        int bytesToRead = bytes.length;
        int readCount = 0;
        while (readCount < bytesToRead) {
            int result = ins.read(bytes, readCount, bytesToRead - readCount);
            // Stream意外结束
            if (result == -1) {
                throw new Exception("异常：InputStream意外结束!");
            }
            readCount += result;
        }
    }

    //接收包含字节个数的一组字节。
    private static byte[] recv(Socket socket) throws Exception{
        byte[] countBytes = new byte[4];
        readFully(socket, countBytes);
        int count = Utils.byteArrayToInt(countBytes);
        byte[] dataBytes = new byte[count];
        readFully(socket, dataBytes);
        return dataBytes;
    }

    //recv(.)的另一个版本，采用ObjectInputStream
    private static byte[] recv0(Socket socket) throws Exception{
        InputStream is = socket.getInputStream();
        ObjectInputStream ois = new ObjectInputStream(is);
        int byteLength = ois.readInt();
        byte[] byteArray = new byte[byteLength];
        ois.readFully(byteArray);
        return byteArray;
    }


    //接收一个字符串
    public static String recvString(Socket socket) throws Exception{
        byte[] ibytes = recv(socket);
        String msg = new String(ibytes,0, ibytes.length,"utf-8");
        return msg;
    }

    //接收一个对象，该对象必须实现Serializable接口
    public static Object recvObject(Socket socket) throws Exception{
        byte[] bytes = Receiver.recv(socket);
        Object obj =  Utils.byteArrayToObjSerializable(bytes);
        return obj;
    }

    //recvObject(.)的另一个版本，采用ObjectInputStream
    public static Object recvObject0(Socket socket) throws Exception{
        InputStream is = socket.getInputStream();
        ObjectInputStream ois = new ObjectInputStream(is);
        Object obj = ois.readObject();
        return obj;
    }

    // recvObject(.)的JSON版本。
    // 接收Json字符串，-> Object
    // 不要求object实现Serializable接口
    public static <T> T recvObjectByJson(Socket socket, Class<T> clazz) throws Exception{
        String msg = recvString(socket);
        <fim_suffix>
        return t;
    }

    // recvObject(.)的JSON版本。
    // 接收Json字符串，-> Object List
    // 不要求object实现Serializable接口
    public static <T> List<T> recvObjectListByJson(Socket socket, Class<T> clazz) throws Exception{
        String msg = recvString(socket);
        List<T> t = JSON.parseArray(msg, clazz);
        return t;
    }

    //接收一个文件，支持大文件
    public static void recvFile(Socket socket, String savedFileName) throws Exception{
        InputStream is = socket.getInputStream();
        FileOutputStream fos = new FileOutputStream(new File(savedFileName));
        //先获取一个长整数的8个字节，该长整数表示文件的长度
        byte[] lengthBytes = new byte[8];
        readFully(socket, lengthBytes);
        //要接收的总的字节数
        long restBytesToRead = Utils.byteArrayToLong(lengthBytes);
        //缓冲区大小为1MB
        int bufSize = 1024*1024;
        byte[] dataBytes = new byte[bufSize];
        do {
            //接收最后一组字节时可能需要调整缓冲区大小
            if (bufSize > restBytesToRead) {
                bufSize = (int)restBytesToRead;
                dataBytes = new byte[bufSize];
            }
            //填满缓冲区
            readFully(socket, dataBytes);
            //将缓冲区内容存入文件
            fos.write(dataBytes);
            //计算剩余尚未接收的字节数
            restBytesToRead -= bufSize;
        }while(restBytesToRead > 0);
        fos.close();
    }
}
<fim_middle>