<filename>src/main/java/com/github/jvm/io/protocol/c2d/codc/hessian/HessianCodec.java<fim_prefix>

        package com.github.jvm.io.protocol.c2d.codc.hessian;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.github.jvm.io.protocol.c2d.message.C2DHeader;
import com.github.jvm.io.protocol.c2d.message.C2DMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Hessian序列化
 *
 * @author : Crab2Died
 * 2017/12/14  14:57:07
 */
@SuppressWarnings("all")
public class HessianCodec {

    public static byte[] encode(Object obj) throws IOException {

        HessianOutput ho = null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            ho = new HessianOutput(baos);
            ho.writeObject(obj);
            baos.flush();
            return baos.toByteArray();
        } finally {
            if (ho != null) {
                ho.close();
            }
        }
    }

    public static <T> T decode(byte[] bytes, Class<T> clazz) throws IOException {

        HessianInput hi = null;
<fim_suffix>
    }


    public static void main(String... args) {
        C2DMessage message = new C2DMessage();
        C2DHeader header = new C2DHeader();
        header.setMagic(323435);
        header.setSerial(45678L);
        Map<String, Object> map = new HashMap<>();
        header.setAttachment(map);
        map.put("a", new Object());
        map.put("b", "a");
        message.setBody(header);
        message.setHeader(header);
        try {
            System.out.println("原对象为：" + message);
            byte[] bytes = encode(message);
            System.out.println("序列化后byte数组长度为：" + bytes.length);
            C2DMessage _d = (C2DMessage)decode(bytes, Object.class);
            System.out.println("反序列化后对象为：" + _d);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
<fim_middle>