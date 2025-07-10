<filename>wechat-message-handler-master/src/main/java/com/aaron/springcloud/wx/util/WxResourceUtil.java<fim_prefix>

        package com.aaron.springcloud.wx.util;

import com.aaron.springcloud.wx.cache.CacheItem;
import com.aaron.springcloud.wx.cache.impl.memory.AccessTokenCacheMemoryRepository;
import com.aaron.springcloud.wx.cache.impl.memory.MediaMemoryCacheRepository;
import com.aaron.springcloud.wx.cache.impl.memory.QrCodeMemoryCacheRepository;
import com.aaron.springcloud.wx.config.AppConfig;
import com.aaron.springcloud.wx.constants.MessageUrl;
import com.aaron.springcloud.wx.domain.AccessTokenCacheItem;
import com.aaron.springcloud.wx.domain.MediaCacheItem;
import com.aaron.springcloud.wx.domain.MediaResourceRequest;
import com.aaron.springcloud.wx.domain.MiniProgramQrCodeParam;
import com.aaron.springcloud.wx.domain.QrCode;
import com.aaron.springcloud.wx.domain.QrCodeCacheItem;
import com.aaron.springcloud.wx.domain.QrCodeRequest;
import com.aaron.springcloud.wx.domain.TemporaryQrCodeRequest;
import com.aaron.springcloud.wx.domain.UserOpenId;
import com.aaron.springcloud.wx.domain.UserOpenIdList;
import com.aaron.springcloud.wx.exception.WxException;
import com.aaron.springcloud.wx.menu.MenuButton;
import com.aaron.springcloud.wx.message.CostumerMessage;
import com.aaron.springcloud.wx.message.TemplateMessage;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 获取微信端资源工具类
 *
 * @author FengHaixin
 * @description 一句话描述该文件的用途
 * @date 2018-11-07
 */
public final class WxResourceUtil extends BaseUtil
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WxResourceUtil.class);

    /**
     * 默认的媒体资源内存缓存实现
     */
    private static final CacheItem<MediaCacheItem> DEFAULT_MEDIA_CACHE = new MediaMemoryCacheRepository();

    /**
     * 默认的二维码内存缓存实现
     */
    private static final CacheItem<QrCodeCacheItem> DEFAULT_QR_CODE_CACHE = new QrCodeMemoryCacheRepository();

    /**
     * 默认的accessToken内存缓存实现
     */
    private static final CacheItem<AccessTokenCacheItem> DEFAULT_ACCESS_TOKEN_CACHE = new AccessTokenCacheMemoryRepository();


    private WxResourceUtil()
    {

    }


    /**
     * 获取小程序或者公众号对应的AccessToken，此token是微信接口调用的凭证
     * <p>
     * <p>
     * <p>
     * 默认使用内存缓存
     *
     * @param appConfig AppConfig：小程序或者公众号的配置信息
     *
     * @return 返回accesstoken
     */
    public static String getAccessToken(AppConfig appConfig)
    {

        return getAccessToken(appConfig, DEFAULT_ACCESS_TOKEN_CACHE);

    }


    /**
     * 获取小程序或者公众号对应的AccessToken，此token是微信接口调用的凭证
     * <p>
     * <p>
     * <p>
     * 不适用缓存，每次从微信获取最新的数据
     *
     * @param appConfig AppConfig：小程序或者公众号的配置信息
     *
     * @return 返回accesstoken
     */
    public static String getAccessTokenWithoutCache(AppConfig appConfig)
    {
        return doGetAccessToken(appConfig).getAccessToken();
    }


    /**
     * 获取小程序或者公众号对应的AccessToken，此token是微信接口调用的凭证
     * <p>
     * 该接口可以传入自定义的缓存实现
     *
     * @param appConfig AppConfig：小程序或者公众号的配置信息
     * @param cacheRepository CacheItem<AccessTokenCacheItem>：自定义缓存实现
     *
     * @return 返回accesstoken
     */
    public static String getAccessToken(AppConfig appConfig, CacheItem<AccessTokenCacheItem> cacheRepository)
    {

        String appId = appConfig.getAppId();
        String accessToken = cacheRepository.get(appId);
        if (StringUtils.isNotEmpty(accessToken))
        {
            return accessToken;
        }

        synchronized (appId.intern())
        {
            accessToken = cacheRepository.get(appId);
            if (StringUtils.isNotEmpty(accessToken))
            {

                return accessToken;
            }

            AccessTokenCacheItem cacheItem = doGetAccessToken(appConfig);

            //保存缓存
            cacheRepository.save(appId, cacheItem);

            return cacheItem.getAccessToken();
        }
    }


    /**
     * 每次最多拉取100条
     */
    public static void batchGetUserInfo(String appId, String accessToken)
    {
        String url = MessageUrl.BATCH_GET_USER_INFO_URL + accessToken;

        File file = new File("/Users/fenghaixin/Desktop/out/weixin.sql");
        //已经拉取的数量
        int hasFetchTotal = 0;
        int total = 0;

        int tryCount = 0;
        do
        {
            JSONObject subscribers;
            try
            {
                subscribers = getSubscriberOpenId(accessToken);
                if (!isSuccess(subscribers))
                {
                    continue;
                }

                total = subscribers.getInteger("total");
                hasFetchTotal += subscribers.getInteger("count");

                List<UserOpenId> userOpenIds = subscribers.getJSONObject("data")
                        .getJSONArray("openid")
                        .stream()
                        .map(openId -> new UserOpenId(String.valueOf(openId)))
                        .collect(Collectors.toList());

                int size = userOpenIds.size();

                int inTryCount = 0;
                int fromIndex = 0;
                int toIndex;
                do
                {

                    toIndex = fromIndex + 100 <= size ? fromIndex + 100 : size;
                    UserOpenIdList userOpenIdList = new UserOpenIdList(userOpenIds.subList(fromIndex, toIndex));

                    HttpEntity<UserOpenIdList> requestEntity = new HttpEntity<>(userOpenIdList, DEFAULT_HEADER);

                    try
                    {
                        JSONObject jsonObject = extractResponse(REST_TEMPLATE.postForEntity(url, requestEntity, String.class));

                        if (isSuccess(jsonObject))
                        {
                            JSONArray jsonArray = jsonObject.getJSONArray("user_info_list");

                            StringBuilder stringBuilder = new StringBuilder();
                            jsonArray.forEach(user -> {

                                JSONObject object = (JSONObject)user;
                                String unionId = object.getString("unionid");
                                String openId = object.getString("openid");
                                String nickName = object.getString("nickname");
                                String headImgUrl = object.getString("headimgurl");
                                stringBuilder.append(
                                        "INSERT INTO xmkp_user.WECHAT_USER_INFO (union_id, app_id, open_id, nick_name, head_url) VALUES ('")
                                                .append(unionId)
                                                .append("', '")
                                             .append(appId)
                                        .append("', '")
                                             .append(openId)
                                        .append("', '")
                                             .append(nickName)
                                        .append("', '")
                                             .append(headImgUrl)
                                        .append("');\n");
                            });

                            try
                            {
                                FileUtils.write(file, stringBuilder.toString(), "utf-8", true);
                            }
                            catch (IOException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                    catch (RestClientException e)
                    {
                        inTryCount++;
                    }

                    fromIndex += 100;
                }
                while (toIndex < size && inTryCount <= 3);

            }
            catch (Exception e)
            {
                tryCount++;
            }

        }
        while (hasFetchTotal < total && tryCount <= 3);

    }


    /**
     * 获取关注用户的openid列表
     */
    private static JSONObject getSubscriberOpenId(String accessToken)
    {
        String url = MessageUrl.GET_SUBSCRIBER_OPENID_LIST_URL + accessToken;

        ResponseEntity<String> forEntity = REST_TEMPLATE.getForEntity(url, String.class);

        return extractResponse(forEntity);
    }


    /**
     * 服务号模板消息发送接口
     *
     * @param message TemplateMessage：要发送的模板消息内容
     * @param accessToken String：接口调用凭证accessToken
     *
     * @return 发送成功时返回true，否则返回false
     */
    public static boolean sendTemplateMessage(TemplateMessage message, String accessToken)
    {

        try
        {
            HttpEntity<TemplateMessage> requestEntity = new HttpEntity<>(message, DEFAULT_HEADER);
            String url = MessageUrl.SEND_TEMPLATE_MESSAGE_URL + accessToken;
            JSONObject jsonObject = extractResponse(REST_TEMPLATE.postForEntity(url, requestEntity, String.class));

            return isSuccess(jsonObject);
        }
        catch (RestClientException e)
        {
            LOGGER.error("发送模板消息异常", e);
            return false;
        }
    }


    /**
     * 小程序发送模板消息，消息是展示在微信你的服务通知中的
     *
     * @return 发送成功时返回true，否则返回false
     */
    public static boolean sendTemplateMessage()
    {
        return false;

    }


    /**
     * 微信小程序和服务号客服消息发送
     *
     * @param costumerMessage CostumerMessage：待发送的客服消息
     *
     * @return 发送成功时返回true，否则返回false
     */
    public static boolean sendCustomerMessage(CostumerMessage costumerMessage)
    {
        try
        {

            HttpEntity<CostumerMessage> requestEntity = new HttpEntity<>(costumerMessage, DEFAULT_HEADER);
            JSONObject jsonObject = extractResponse(REST_TEMPLATE.postForEntity(costumerMessage.getUrl(), requestEntity, String.class));

            return isSuccess(jsonObject);
        }
        catch (RestClientException e)
        {
            LOGGER.error("发送客服消息异常", e);
            return false;
        }
    }


    /**
     * 微信媒体资源上传，使用默认的内存缓存
     *
     * @param mediaResource MediaResource：素材资源信息
     * @param accessTokenFun Function<String, String>： 延迟计算获取accessToken的函数式方法
     *
     * @return
     */
    public static String uploadTemporaryMediaResource(MediaResourceRequest mediaResource, Function<String, String> accessTokenFun)
    {
        return uploadTemporaryMediaResource(mediaResource, accessTokenFun, DEFAULT_MEDIA_CACHE);
    }


    /**
     * 微信媒体资源上传，不适用任何缓存
     *
     * @param mediaResource MediaResource：素材资源信息
     * @param accessToken String： accessToken
     *
     * @return
     */
    public static String uploadTemporaryMediaResourceWithoutCache(MediaResourceRequest mediaResource, String accessToken)
    {
        try
        {
            return doUploadTemporaryMediaResource(mediaResource, accessToken);
        }
        catch (IOException e)
        {
            throw new RuntimeException("资源上传错误", e);
        }
    }


    /**
     * 微信媒体资源上传，该方法可以自定义缓存实现
     *
     * @param mediaResource MediaResource：素材资源信息
     * @param accessTokenFun Function<String, String>： 延迟计算获取accessToken的函数式方法
     * @param mediaCacheRepository CacheItem<MediaCacheItem>： 自定义的缓存实现
     *
     * @return 返回media_id
     */
    public static String uploadTemporaryMediaResource(MediaResourceRequest mediaResource,
                                                      Function<String, String> accessTokenFun,
                                                      CacheItem<MediaCacheItem> mediaCacheRepository)
    {
        //尝试优先从缓存中获取
        String cacheKey = mediaResource.getAppId() + mediaResource.getResourceUrl();
        String mediaId = mediaCacheRepository.get(cacheKey);

        if (!StringUtils.isEmpty(mediaId))
        {
            return mediaId;
        }

        synchronized (cacheKey.intern())
        {

            mediaId = mediaCacheRepository.get(cacheKey);

            if (!StringUtils.isEmpty(mediaId))
            {
                return mediaId;
            }

            try
            {

                String accessToken = accessTokenFun.apply(mediaResource.getAppId());
                mediaId = doUploadTemporaryMediaResource(mediaResource, accessToken);

                //写入到缓存中
                mediaCacheRepository.save(cacheKey, new MediaCacheItem(mediaId));

                return mediaId;

            }
            catch (Exception e)
            {
                LOGGER.error("媒体资源上传错误", e);
                throw new RuntimeException("上传媒体资源失败");
            }
        }
    }


    /**
     * 创建永久的服务号二维码
     *
     * @param qrCode QrCode：二维码请求
     * @param accessToken String： accessToken
     *
     * @return 可直接访问的二维码地址
     */
    public static String createPermanentQrCodeWithOutCache(QrCode qrCode, String accessToken)
    {
        QrCodeRequest qrCodeRequest = new QrCodeRequest(qrCode.getAppId(), qrCode.getSceneParam());

        return doCreateQrCodeWithoutCache(qrCodeRequest, accessToken);
    }


    /**
     * 创建永久的服务号二维码，带缓存
     *
     * @param qrCode QrCode：二维码请求
     * @param accessTokenFun Function<String, String>： 延迟计算获取accessToken的函数式方法
     *
     * @return 可直接访问的二维码地址
     */
    public static String createPermanentQrCode(QrCode qrCode, Function<String, String> accessTokenFun)
    {

        return createPermanentQrCode(qrCode, accessTokenFun, DEFAULT_QR_CODE_CACHE);
    }


    /**
     * 创建永久的服务号二维码，可以自定义缓存实现
     *
     * @param qrCode QrCode：二维码请求
     * @param accessTokenFun Function<String, String>： 延迟计算获取accessToken的函数式方法
     * @param qrCodeCacheRepository CacheItem<QrCodeCacheItem>：自定义的缓存实现
     *
     * @return 可直接访问的二维码地址
     */
    public static String createPermanentQrCode(QrCode qrCode,
                                               Function<String, String> accessTokenFun,
                                               CacheItem<QrCodeCacheItem> qrCodeCacheRepository)
    {

        QrCodeRequest qrCodeRequest = new QrCodeRequest(qrCode.getAppId(), qrCode.getSceneParam());

        return doCreateQrCode(qrCodeRequest, accessTokenFun, qrCodeCacheRepository);
    }


    /**
     * 创建临时的服务号二维码，不使用缓存
     *
     * @param qrCode QrCode：二维码请求
     * @param accessToken String： accessToken
     *
     * @return 可直接访问的二维码地址
     */
    public static String createTemporaryQrCodeWithOutCache(QrCode qrCode, String accessToken)
    {
        TemporaryQrCodeRequest qrCodeRequest = new TemporaryQrCodeRequest(qrCode.getAppId(), qrCode.getSceneParam());

        return doCreateQrCodeWithoutCache(qrCodeRequest, accessToken);
    }


    /**
     * 创建临时的服务号二维码，使用默认的内存缓存
     *
     * @param qrCode QrCode：二维码请求
     * @param accessTokenFun Function<String, String>： 延迟计算获取accessToken的函数式方法
     *
     * @return 可直接访问的二维码地址
     */
    public static String createTemporaryQrCode(QrCode qrCode, Function<String, String> accessTokenFun)
    {
        return createTemporaryQrCode(qrCode, accessTokenFun, DEFAULT_QR_CODE_CACHE);
    }


    /**
     * 创建临时的服务号二维码，可以自定义缓存实现
     *
     * @param qrCode QrCode：二维码请求
     * @param accessTokenFun Function<String, String>： 延迟计算获取accessToken的函数式方法
     * @param qrCodeCacheRepository CacheItem<QrCodeCacheItem>：自定义的缓存实现
     *
     * @return 可直接访问的二维码地址
     */
    public static String createTemporaryQrCode(QrCode qrCode,
                                               Function<String, String> accessTokenFun,
                                               CacheItem<QrCodeCacheItem> qrCodeCacheRepository)
    {
        TemporaryQrCodeRequest qrCodeRequest = new TemporaryQrCodeRequest(qrCode.getAppId(), qrCode.getSceneParam());

        return doCreateQrCode(qrCodeRequest, accessTokenFun, qrCodeCacheRepository);
    }


    /**
     * 创建小程序二维码
     *
     * @param qrCodeParam MiniProgramQrCodeParam：小程序二维码创建的参数值
     * @param accessToken String：微信服务器要求的accessToken
     *
     * @return 小程序二维码的字节数组
     */
    public static byte[] createMiniProgramQrCode(MiniProgramQrCodeParam qrCodeParam, String accessToken)
    {

        String requestUrl = MessageUrl.MINIPROGRAM_QRCODE_URL + accessToken;

        HttpEntity<Object> requestEntity = new HttpEntity<>(qrCodeParam, DEFAULT_HEADER);

        //小程序二维码特殊些，如果处理正确的话，返回的是byte数组
        ResponseEntity<byte[]> responseEntity = REST_TEMPLATE.postForEntity(requestUrl, requestEntity, byte[].class);

        if (responseEntity.getBody() == null)
        {
            throw new RuntimeException("小程序二维码生成失败");
        }

        String result = new String(responseEntity.getBody(), StandardCharsets.UTF_8);
        //请求失败
        if (result.contains("errcode"))
        {
            JSONObject jsonObject = JSON.parseObject(result);

            LOGGER.error("小程序二维码生成失败，错误原因：" + jsonObject.getString("errmsg"));

            throw new WxException(jsonObject.getInteger("errcode"), "小程序二维码生成失败" + jsonObject.getString("errmsg"));
        }

        return responseEntity.getBody();
    }


    /**
     * 创建服务号菜单，该方法一般不常用
     *
     * @param menuButton MenuButton：服务号菜单数据
     *
     * @return 创建成功返回true，其他返回false
     */
    public static boolean createMenu(MenuButton menuButton)
    {
        try
        {
            HttpEntity<MenuButton> httpEntity = new HttpEntity<>(menuButton, DEFAULT_HEADER);
            JSONObject jsonObject = extractResponse(REST_TEMPLATE.postForEntity(menuButton.getUrl(), httpEntity, String.class));

            return isSuccess(jsonObject);
        }
        catch (RestClientException e)
        {
            LOGGER.error("菜单创建失败", e);
            return false;
        }
    }


    private static String doCreateQrCode(QrCodeRequest qrCodeRequest,
                                         Function<String, String> accessTokenFun,
                                         CacheItem<QrCodeCacheItem> cacheRepository)
    {

        String sceneStr = qrCodeRequest.getActionInfo().getScene().getSceneStr();

        String cacheKey = qrCodeRequest.getAppId() + sceneStr;

        String qrCodeUrl = cacheRepository.get(cacheKey);

        if (StringUtils.isNotEmpty(qrCodeUrl))
        {
            return qrCodeUrl;
        }

        synchronized (cacheKey.intern())
        {
            qrCodeUrl = cacheRepository.get(cacheKey);

            if (StringUtils.isNotEmpty(qrCodeUrl))
            {
                return qrCodeUrl;
            }
            else
            {
                String accessToken = accessTokenFun.apply(qrCodeRequest.getAppId());
                String url = doCreateQrCodeWithoutCache(qrCodeRequest, accessToken);

                saveToCache(qrCodeRequest, url, cacheRepository);

                return url;
            }
        }

    }


    private static String doCreateQrCodeWithoutCache(QrCodeRequest qrCodeRequest, String accessToken)
    {
        String requestUrl = MessageUrl.CREATE_QRCODE_TICKET_URL + accessToken;

        HttpEntity<QrCodeRequest> httpEntity = new HttpEntity<>(qrCodeRequest, DEFAULT_HEADER);

        //拿Ticket
        JSONObject jsonObject = extractResponse(REST_TEMPLATE.postForEntity(requestUrl, httpEntity, String.class));

        if (!isSuccess(jsonObject))
        {
            throw new WxException(jsonObject.getInteger("errcode"), jsonObject.getString("errmsg"));
        }

        try
        {
            return MessageUrl.QRCODE_URL + URLEncoder.encode(jsonObject.getString("ticket"), "UTF-8");
        }
        catch (Exception e)
        {
            throw new RuntimeException("出现异常", e);
        }
    }


    private static AccessTokenCacheItem doGetAccessToken(AppConfig appConfig)
    {
        String accessToken;
        String url = MessageUrl.ACCESSTOKEN_URL.replaceFirst("\\{}", appConfig.getAppId()).replaceFirst("", appConfig.getAppSecret());

        JSONObject jsonObject = extractResponse(REST_TEMPLATE.getForEntity(url, String.class));

        if (isSuccess(jsonObject))
        {
            //expires_in
            accessToken = jsonObject.getString("access_token");
            int expiresIn = jsonObject.getIntValue("expires_in");

            return new AccessTokenCacheItem(accessToken, expiresIn);
        }

        throw new RuntimeException("accessToken获取失败");
    }


    private static String doUploadTemporaryMediaResource(MediaResourceRequest mediaResource, String accessToken) throws IOException
    {
        String path = mediaResource.getResourceUrl();
<fim_suffix>
        byte[] byteArray = IOUtils.toByteArray(new URL(path));
        ContentBody contentBody = new ByteArrayBody(byteArray, FilenameUtils.getName(path));

        FormBodyPart bodyPartBuilder = FormBodyPartBuilder.create(contentBody.getFilename(), contentBody).build();

        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        multipartEntityBuilder.addPart(bodyPartBuilder);
        org.apache.http.HttpEntity requestEntity = multipartEntityBuilder.build();

        String url = MessageUrl.UPLOAD_TEMP_MEDIA_URL + accessToken + "&type=" + mediaResource.getType();
        HttpPost post = new HttpPost(url);
        post.addHeader("Connection", "Keep-Alive");
        post.addHeader("Cache-Control", "no-cache");
        post.setEntity(requestEntity);

        HttpClient client = HttpClients.createDefault();
        HttpResponse httpResponse = client.execute(post);
        int status = httpResponse.getStatusLine().getStatusCode();

        if (status != HttpStatus.SC_OK)
        {
            throw new RuntimeException("请求失败");
        }

        String result = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");

        JSONObject jsonObject = JSON.parseObject(result);

        if (jsonObject.containsKey("errcode"))
        {
            //请求失败
            LOGGER.error("媒体资源上传失败，错误原因：" + jsonObject.getString("errmsg"));

            throw new WxException(jsonObject.getInteger("errcode"), jsonObject.getString("errmsg"));
        }

        LOGGER.info("媒体资源上传成功：{}", result);

        return jsonObject.getString("media_id");
    }


    private static String saveToCache(QrCodeRequest qrCodeRequest, String qrCodeUrl, CacheItem<QrCodeCacheItem> qrCodeCacheRepository)
    {
        QrCodeCacheItem cacheItem;

        if (qrCodeRequest instanceof TemporaryQrCodeRequest)
        {
            cacheItem = new QrCodeCacheItem(qrCodeUrl, ((TemporaryQrCodeRequest)qrCodeRequest).getExpireSeconds());
        }
        else
        {
            cacheItem = new QrCodeCacheItem(qrCodeUrl);
        }

        String sceneStr = qrCodeRequest.getActionInfo().getScene().getSceneStr();

        String cacheKey = qrCodeRequest.getAppId() + sceneStr;

        qrCodeCacheRepository.save(cacheKey, cacheItem);

        return qrCodeUrl;
    }
}
<fim_middle>