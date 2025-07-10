<filename>xrpc-common/src/main/java/com/dzgu/xrpc/serializer/hessian/HessianSerializer1.java<fim_prefix>

        package com.dzgu.xrpc.serializer.hessian;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.dzgu.xrpc.consts.enums.SerializerTypeEnum;
import com.dzgu.xrpc.exception.SerializeException;
import com.dzgu.xrpc.serializer.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * @description: Hessian 序列化
 */
public class HessianSerializer1 implements Serializer {
    @Override
    public SerializerTypeEnum getSerializerAlgorithm() {
        return SerializerTypeEnum.HESSIAN;
    }

    @Override
    public byte[] serialize(Object object) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            HessianOutput hessianOutput = new HessianOutput(byteArrayOutputStream);
            hessianOutput.writeObject(object);
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new SerializeException("Hessian Serialization failed:", e.getMessage());
        }

    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
           <fim_suffix>
    }
}
<fim_middle>