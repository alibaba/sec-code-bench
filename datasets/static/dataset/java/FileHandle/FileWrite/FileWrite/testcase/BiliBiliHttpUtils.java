<filename>src/main/java/top/huashengshu/bilibili/utils/http/BiliBiliHttpUtils.java<fim_prefix>

        package top.huashengshu.bilibili.utils.http;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
public class BiliBiliHttpUtils {

    /**
     * 创建目录：如果不存在则创建然后返回true，如果存在直接返回true
     * @param destDirName  目录名全路径 盘符驱动+路径
     * @return
     */
    public static boolean createDirect(String destDirName) {
        File file = new File(destDirName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return true;
    }


    /**
     * 下载文件的作用，需要3个参数 资源原路径，真正下载的url，保存本地的路径
     *
     * @param refererUrl 视频的参考路径，
     * @param url        真正下载的url链接，可以是m4s文件和audio文件，下载后需要合并
     * @param savePath   保存的路径
     * @return 下载成功则返回true，中间报异常则进行自旋下载文件
     */
    public static boolean downloadFile(String refererUrl, String url, String savePath) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("referer",refererUrl);//设置请求头
        HttpEntity<String> entity = new HttpEntity<String>("", headers);
        ResponseEntity<Resource> in = restTemplate.exchange(url, HttpMethod.GET, entity, Resource.class);
        try (InputStream is = in.getBody().getInputStream()) {//java9新特性自动关闭流
            boolean flag = createDirect(savePath);
            if (flag) {
                <fim_suffix>
            }
        } catch (IOException e) {
            downloadFile(refererUrl,url,savePath);//异常则进行重新下载
        }
        return true;
    }


    /**
     * 获取url中的 bvid
     *
     * @param url
     * @return
     * @throws Exception
     */
    public static String getBVID(String url) throws Exception {
        Pattern BVIDPartPattern = Pattern.compile("BV[a-z|0-9|A-Z]*");
        Matcher matcher = BVIDPartPattern.matcher(url);
        String bvid;
        if (matcher.find()) {
            bvid = matcher.group();
        } else {
            throw new Exception("传入的url中不包含bvid");
        }
        return bvid;
    }

    /**
     * 传入视频的播放地址，例如url: "https://www.bilibili.com/video/BV1m4411H7pi"  获取其中包含BV的这串字符串
     *
     * @param url
     * @return
     * @throws Exception
     */
    public static JSONObject getCidJSON(String url) throws Exception {
        String bvid = getBVID(url);
        //从url中转换成最终请求的url接口
        String requestUrl = new StringBuilder("http://api.bilibili.com/x/web-interface/view?bvid=").append(bvid).toString();
        String jsonString = new RestTemplate().getForEntity(requestUrl, String.class).getBody();
        JSONObject parse = JSONObject.parseObject(jsonString);
        return parse.getJSONObject("data");//返回data部分
    }


    /**
     * 传入cid的json数组，获取所有下载链接--》一个cid对应一个视频，一个视频有多个清晰度的播放源
     *
     * @param cidJSONObject
     * @return
     */
    public static List<JSONObject> getPlayUrlList(JSONObject cidJSONObject) {
        String bvid = cidJSONObject.getString("bvid");//视频的id
        String title = cidJSONObject.getString("title");//标题--》用来做文件夹
        String picUrl = cidJSONObject.getString("pic");//封面
        JSONArray pages = cidJSONObject.getJSONArray("pages");//包含这一系列视频的cid内容

        try (Stream<JSONObject> jsonObjectStream = pages.parallelStream()
                .map(o -> {
                    JSONObject obj = (JSONObject) o;
                    String cid = obj.getString("cid");//获取cid
                    String part = obj.getString("part");//获取视频名  前缀

                    //获取播放地址的api
                    String requestUrl = new StringBuilder("https://api.bilibili.com/x/player/playurl?cid=").append(cid)
                            .append("&bvid=").append(bvid).append("&qn=80&fnver=0&fnval=16&fourk=1").toString();
                    System.out.println(requestUrl);

                    String jsonString = new RestTemplate().getForEntity(requestUrl, String.class).getBody();
                    JSONObject data = JSONObject.parseObject(jsonString).getJSONObject("data");//获取api返回的jaon中的data对象
                    /**
                     * 添加自定义的属性，方便保存
                     */
                    data.put("cid", cid);//将cid添加到返回的数据中
                    data.put("directName", title);//添加视频的标题作为文件夹
                    data.put("videoName", part);//添加视频的名称
                    return data;//将添加了自定义属性的json返回，作为新的流
                })) {
//        System.out.println(cidJSONObject);

            return jsonObjectStream.collect(Collectors.toList());//将收集到的url封装起来
        }
    }

}
<fim_middle>