<filename>rtsp-face-master/src/main/java/com/zs/rtspface/ConvertVideoPakcet.java<fim_prefix>

package com.zs.rtspface;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Component
public class ConvertVideoPakcet implements ApplicationRunner {
    public Process process;

    public Integer pushVideoAsRTSP(String id, String fileName){
        int flag = -1;
        // ffmpeg位置，最好写在配置文件中
//        String ffmpegPath = "E:\\webset\\ffmpeg\\ffmpeg-20200213-6d37ca8-win64-static\\bin\\";
        String ffmpegPath = "";
        try {
            // 视频切换时，先销毁进程，全局变量Process process，方便进程销毁重启，即切换推流视频
            if(process != null){
                process.destroy();
                System.out.println(">>>>>>>>>>推流视频切换<<<<<<<<<<");
            }
            // cmd命令拼接，注意命令中存在空格
            <fim_suffix>

            // 运行cmd命令，获取其进程
            process = Runtime.getRuntime().exec(command);
            // 输出ffmpeg推流日志
            BufferedReader br= new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line = "";
            while ((line = br.readLine()) != null) {
                System.out.println("视频推流信息[" + line + "]");
            }
            flag = process.waitFor();
        }catch (Exception e){
            e.printStackTrace();
        }
        return flag;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ConvertVideoPakcet convertVideoPakcet = new ConvertVideoPakcet();
        convertVideoPakcet.pushVideoAsRTSP("rtsp://admin:QJISUS4008950110@192.168.10.215:554/Streaming/Channels/101", "http://localhost/rtsp/receive");

    }
}
<fim_middle>
