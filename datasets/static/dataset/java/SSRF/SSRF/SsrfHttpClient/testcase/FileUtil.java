<filename>Mockexam-Server-master/background/eladmin-system/src/main/java/me/zhengjie/modules/mockexam/utils/FileUtil.java<fim_prefix>

        package me.zhengjie.modules.mockexam.utils;

import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.annotation.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * @author :         Mingxuan_x
 * @version :        1.0
 * @Description: 文件处理工具类
 * @Telephone :      15135964789
 * @createDate :     2021/4/10 23:08
 * @updateUser :     Mingxuan_x
 * @updateDate :     2021/4/10 23:08
 * @updateRemark :   修改内容
 **/
@Slf4j
public class FileUtil {

    /**
     * 文件下载
     * @param picUrl  文件资源链接
     * @param imgPath 保存路径
     * @return
     */
    public static boolean download(String picUrl, String imgPath) {
        <fim_suffix>
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpGet get = new HttpGet(picUrl);
            HttpResponse response = httpclient.execute(get);
            HttpEntity entity = response.getEntity();
            InputStream in = entity.getContent();
            try (FileOutputStream fout = new FileOutputStream(imgPath)) {
                int l = -1;
                byte[] tmp = new byte[1024];
                while ((l = in.read(tmp)) != -1) {
                    fout.write(tmp, 0, l);
                }
                fout.flush();
                return true;
            }
        } catch (Exception e1) {
            log.error("errorMessage",e1);
            return false;
        }

    }

    public static void main(String[] args) throws Exception {
        FileUtil.download("http://www.7-zip.org/a/7z1604.exe", "D:/upload/a.exe");
    }
}
<fim_middle>