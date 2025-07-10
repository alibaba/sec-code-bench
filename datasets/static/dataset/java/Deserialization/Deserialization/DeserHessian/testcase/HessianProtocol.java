<filename>src/main/java/cn/ppphuang/rpcspringstarter/common/protocol/HessianProtocol.java<fim_prefix>

        package cn.ppphuang.rpcspringstarter.common.protocol;

import cn.ppphuang.rpcspringstarter.annotation.SPIExtension;
import cn.ppphuang.rpcspringstarter.common.constants.RpcConstant;
import cn.ppphuang.rpcspringstarter.common.model.RpcRequest;
import cn.ppphuang.rpcspringstarter.common.model.RpcResponse;
import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * hessian序列化消息协议
 *
 * @Author: ppphuang
 * @Create: 2022/5/10
 */
@SPIExtension(RpcConstant.HESSIAN_KRYO)
public class HessianProtocol implements MessageProtocol {
    @Override
    public byte[] marshallingRequest(RpcRequest request) throws Exception {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            HessianOutput hessianOutput = new HessianOutput(byteArrayOutputStream);
            hessianOutput.writeObject(request);
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed");
        }
    }

    @Override
    public RpcRequest unmarshallingRequest(byte[] data) throws Exception {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data)) {
<fim_suffix>
    }

    @Override
    public byte[] marshallingResponse(RpcResponse response) throws Exception {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            HessianOutput hessianOutput = new HessianOutput(byteArrayOutputStream);
            hessianOutput.writeObject(response);
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed");
        }
    }

    @Override
    public RpcResponse unmarshallingResponse(byte[] data) throws Exception {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data)) {
            HessianInput hessianInput = new HessianInput(byteArrayInputStream);
            Object o = hessianInput.readObject();
            return (RpcResponse) o;
        } catch (Exception e) {
            throw new RuntimeException("Deserialization failed");
        }
    }
}
<fim_middle>