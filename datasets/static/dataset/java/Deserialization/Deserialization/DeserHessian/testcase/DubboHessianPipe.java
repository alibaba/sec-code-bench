<filename>pipe-dubbo/src/main/java/com/wangyin/ak47/pipes/dubbo/DubboHessianPipe.java<fim_prefix>

        package com.wangyin.ak47.pipes.dubbo;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.rpc.protocol.dubbo.DubboCodec;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianFactory;
import com.wangyin.ak47.common.CollectionUtil;
import com.wangyin.ak47.common.Logger;
import com.wangyin.ak47.common.YmlUtil;
import com.wangyin.ak47.core.HandlerContext;
import com.wangyin.ak47.core.Message;
import com.wangyin.ak47.core.Request;
import com.wangyin.ak47.core.Response;
import com.wangyin.ak47.core.exception.Ak47RuntimeException;
import com.wangyin.ak47.pipes.dubbo.AbstractDubboPipe;
import com.wangyin.ak47.pipes.dubbo.DubboData;
import com.wangyin.ak47.pipes.dubbo.DubboHeader;

/**
 *
 * Dubbo-protocol + Hessian2.0-serialize
 *
 * @author hubingyin
 *
 */
public final class DubboHessianPipe extends AbstractDubboPipe<DubboHessianRequest, DubboHessianResponse> {

    private static final Logger log = new Logger(DubboHessianPipe.class);

    public static final byte HESSIAN_SERIALIZATION_ID = 0x02;

    public static final String DEFAULT_DUBBO_VERSION = Version.getVersion(DubboCodec.class, Version.getVersion());
    public static final String DEFAULT_METHOD_VERSION = "0.0.0";

    public static final byte RESPONSE_WITH_EXCEPTION = 0;
    public static final byte RESPONSE_VALUE = 1;
    public static final byte RESPONSE_NULL_VALUE = 2;

    private HessianFactory hessianFactory = new HessianFactory();

    @Override
    public void decodeDubboRequest(DubboData dd, Request<DubboHessianRequest> request) throws Exception {

        DubboHessianRequest dubboreq = new DubboHessianRequest();
        DubboHeader dh = dd.getDubboHeader();
        byte flag = dh.getFlag();
        if( (flag & FLAG_EVENT) != 0 ){
            // heart beat
            // do nothing.

        }else if( (flag & SERIALIZATION_MASK) == HESSIAN_SERIALIZATION_ID ){
            // decode hessian
            ByteArrayInputStream bis = new ByteArrayInputStream(dd.getBody());
            Hessian2Input h2in = hessianFactory.createHessian2Input(bis);

            String dubboVersion = h2in.readString();
            dubboreq.setDubboVersion(dubboVersion);

            String service = h2in.readString();
            dubboreq.setService(service);

            String version = h2in.readString();
            dubboreq.setVersion(version);

            String method = h2in.readString();
            dubboreq.setMethod(method);

            // this dont work!
            // Object[] args = h2in.readArguments();
            String argsdesc = h2in.readString();
            Class<?>[] argtypes = ReflectUtils.desc2classArray(argsdesc);
            List<Object> args = new ArrayList<Object>(argtypes.length);
            for(int i=0;i<argtypes.length;i++){
                args.add(h2in.readObject(argtypes[i]));
            }
            dubboreq.setArgs(args);

            @SuppressWarnings("unchecked")
            Map<String, String> attachments = (Map<String, String>) h2in.readObject(Map.class);
            dubboreq.setAttachments(attachments);

            log.debug("decodeDubboRequest dubboVersion:{} service:{} version:{} method:{} args.size:{} attachments:{}",
                    dubboVersion, service, version, method, args.size(), YmlUtil.obj2Yml(attachments));

        }else{
            // not hessian serialization
            throw new Ak47RuntimeException("Unsupport serialization type. flag is " + flag );
        }

        dubboreq.setDubboHeader(dh);
        request.pojo(dubboreq);

    }

    @Override
    public void encodeDubboRequest(Request<DubboHessianRequest> request, DubboData dd) throws Exception {

        DubboHessianRequest dubboreq = request.pojo();
        // set flag
        DubboHeader dh = dubboreq.getDubboHeader();
        dh.setFlag((byte) (FLAG_REQUEST | FLAG_TWOWAY | HESSIAN_SERIALIZATION_ID) );

        // set body
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Hessian2Output h2out = hessianFactory.createHessian2Output(bos);

        String dubboVersion = dubboreq.getDubboVersion();
        if( null == dubboVersion ) dubboVersion = DEFAULT_DUBBO_VERSION;
        h2out.writeString(dubboVersion);

        String service = dubboreq.getService();
        h2out.writeString(service);

        String version = dubboreq.getVersion();
        if( null == version ) version = DEFAULT_METHOD_VERSION;
        h2out.writeString(version);

        String method = dubboreq.getMethod();
        h2out.writeString(method);

        Class<?>[] argtypes = CollectionUtil.objects2Classes( dubboreq.getArgs() );
        String argsdesc = ReflectUtils.getDesc(argtypes);
        h2out.writeString(argsdesc);

        for( Object obj : dubboreq.getArgs() ){
            h2out.writeObject(obj);
        }

        Map<String, String> attachments = dubboreq.getAttachments();
        h2out.writeObject(attachments);

        h2out.close();
        byte[] body = bos.toByteArray();

        dd.setDubboHeader(dubboreq.getDubboHeader());
        dd.setBody(body);

        log.debug("encodeDubboRequest dubboVersion:{} service:{} version:{} method:{} args.size:{} attachments:{}",
                dubboVersion, service, version, method, argtypes.length, YmlUtil.obj2Yml(attachments));

    }

    @Override
    public void decodeDubboResponse(DubboData dd, Response<DubboHessianResponse> response) throws Exception {

        DubboHessianResponse dubbores = new DubboHessianResponse();
        DubboHeader dh = dd.getDubboHeader();
        dubbores.setDubboHeader(dh);

        byte flag = dh.getFlag();
        if( (flag & FLAG_EVENT) != 0 ){
            // heart beat
            // do nothing.

        }else if( (flag & SERIALIZATION_MASK) == HESSIAN_SERIALIZATION_ID ){
            // decode hessian
            ByteArrayInputStream bis = new ByteArrayInputStream(dd.getBody());
<fim_suffix>
            if( resvalue == RESPONSE_WITH_EXCEPTION ){
                dubbores.setResult(h2in.readObject());
            }else if( resvalue == RESPONSE_NULL_VALUE){
                dubbores.setResult(null);
            }else if( resvalue == RESPONSE_VALUE){
                dubbores.setResult(h2in.readObject());
            }else{
                throw new Ak47RuntimeException("Unrecognized response value. resvalue is " + resvalue);
            }

        }else{
            // not hessian serialization
            throw new Ak47RuntimeException("Unsupport serialization type. flag is " + flag );
        }

        response.pojo(dubbores);
    }

    @Override
    public void encodeDubboResponse(Response<DubboHessianResponse> response, DubboData dd) throws Exception {

        DubboHessianResponse dubbores = response.pojo();

        // set flag
        DubboHeader dh = dubbores.getDubboHeader();
        dd.setDubboHeader(dubbores.getDubboHeader());

        // set body
        dh.setFlag((byte) (dh.getFlag() | HESSIAN_SERIALIZATION_ID) );

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Hessian2Output h2out = hessianFactory.createHessian2Output(bos);

        h2out.writeInt(RESPONSE_VALUE);

        h2out.writeObject(dubbores.getResult());

        h2out.close();
        byte[] body = bos.toByteArray();

        dd.setBody(body);

    }


    @Override
    public boolean filterReceivedInStub(
            HandlerContext<DubboHessianResponse, DubboHessianRequest> ctx,
            Message<DubboHessianRequest> msg){

        DubboHessianRequest dubboreq = msg.getPojo();
        byte flag = dubboreq.getDubboHeader().getFlag();
        if( (flag & FLAG_EVENT) != 0 ){
            // heartbeat
            Message<DubboHessianResponse> resmsg = msg.newMessage();
            DubboHessianResponse dubbores = new DubboHessianResponse();
            DubboHeader dh = dubbores.getDubboHeader();
            dh.setFlag((byte)(dh.getFlag()|FLAG_EVENT));
            resmsg.setPojo(dubbores);

            ctx.send(resmsg);
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean filterReceivedInDriver(
            HandlerContext<DubboHessianRequest, DubboHessianResponse> ctx,
            Message<DubboHessianResponse> msg){

        DubboHessianResponse dubbores = msg.getPojo();
        byte flag = dubbores.getDubboHeader().getFlag();
        if( (flag & FLAG_EVENT) != 0 ){
            // heartbeat
            Message<DubboHessianRequest> reqmsg = msg.newMessage();
            DubboHessianRequest dubboreq = new DubboHessianRequest();
            DubboHeader dh = dubboreq.getDubboHeader();
            dh.setFlag((byte)(dh.getFlag()|FLAG_EVENT));
            reqmsg.setPojo(dubboreq);

            ctx.send(reqmsg);
            return true;
        }else{
            return false;
        }
    }


}
<fim_middle>