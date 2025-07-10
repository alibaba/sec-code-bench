<filename>janus-sdk/src/main/java/org/xujin/janus/damon/serializer/impl/Hessian1Serializer2.java<fim_prefix>

        package org.xujin.janus.damon.serializer.impl;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import org.xujin.janus.damon.exception.JanusCmdException;
import org.xujin.janus.damon.serializer.AbstractSerializer;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * hessian serialize
 * @author tbkk 2019-9-26 02:53:29
 */
public class Hessian1Serializer2 extends AbstractSerializer {

    @Override
    public <T> byte[] serialize(T obj){
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        HessianOutput ho = new HessianOutput(os);
        try {
            ho.writeObject(obj);
            ho.flush();
            byte[] result = os.toByteArray();
            return result;
        } catch (IOException e) {
            throw new JanusCmdException(e);
        } finally {
            try {
                ho.close();
            } catch (IOException e) {
                throw new JanusCmdException(e);
            }
            try {
                os.close();
            } catch (IOException e) {
                throw new JanusCmdException(e);
            }
        }
    }

    @Override
    public <T> Object deserialize(byte[] bytes, Class<T> clazz) {
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        <fim_suffix>
    }

}
<fim_middle>