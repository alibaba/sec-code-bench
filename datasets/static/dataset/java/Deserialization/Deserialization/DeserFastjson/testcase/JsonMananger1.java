<filename>android/IMLib/src/main/java/com/zhangwuji/im/server/utils/json/JsonMananger1.java<fim_prefix>

        package com.zhangwuji.im.server.utils.json;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.util.TypeUtils;
import com.zhangwuji.im.DB.entity.Department;
import com.zhangwuji.im.DB.entity.Group;
import com.zhangwuji.im.DB.entity.User;
import com.zhangwuji.im.server.network.http.HttpException;
import com.zhangwuji.im.utils.pinyin.PinYin;

import java.util.List;

/**
 * [JSON解析管理类]
 *
 * @author huxinwu
 * @version 1.0
 * @date 2014-3-5
 *
 **/
public class JsonMananger1 {

    static {
        TypeUtils.compatibleWithJavaBean = true;
    }
    private static final String tag = JsonMananger1.class.getSimpleName();

    /**
     * 将json字符串转换成java对象
     * @param json
     * @param cls
     * @return
     * @throws HttpException
     */
    public static <T> T jsonToBean(String json, Class<T> cls){
        <fim_suffix>
        return JSON.parseObject(json, cl);
    }

    /**
     * 将json字符串转换成java List对象
     * @param json
     * @param cls
     * @return
     * @throws HttpException
     */
    public static <T> List<T> jsonToList(String json, Class<T> cls)  {
        return JSON.parseArray(json, cls);
    }

    /**
     * 将bean对象转化成json字符串
     * @param obj
     * @return
     * @throws HttpException
     */
    public static String beanToJson(Object obj){
        String result = JSON.toJSONString(obj);
        Log.e(tag, "beanToJson: " + result);
        return result;
    }
    public static Group parseGroup(JSONObject json)
    {
        Group groupEntity=jsonToBean(json.toJSONString(),Group.class);
        int timeNow = (int) (System.currentTimeMillis()/1000);
        groupEntity.setMainName(json.getString("name"));
        groupEntity.setCreatorId(json.getInteger("creator"));
        groupEntity.setPeerId(json.getInteger("id"));
        groupEntity.setGroupType(json.getInteger("type"));
        groupEntity.setUserList(json.getString("userlist"));
        // may be not good place
        PinYin.getPinYin(groupEntity.getMainName(), groupEntity.getPinyinElement());
        return  groupEntity;
    }
    public static Department parseDepartment(JSONObject json)
    {
        Department department=jsonToBean(json.toJSONString(),Department.class);
        int timeNow = (int) (System.currentTimeMillis()/1000);
        department.setCreated(timeNow);
        department.setUpdated(timeNow);
        PinYin.getPinYin(department.getDepartName(), department.getPinyinElement());
        return  department;
    }

    public static User parseUser(JSONObject json)
    {
        User userEntity = new User();
        int timeNow = (int) (System.currentTimeMillis()/1000);
        User tempUser=jsonToBean(json.toJSONString(),User.class);
        tempUser.setGender(json.getIntValue("sex"));
        tempUser.setPeerId(json.getIntValue("id"));
        tempUser.setMainName(json.getString("nickname"));
        tempUser.setPinyinName(json.getString("domain"));
        tempUser.setCreated(timeNow);
        tempUser.setUpdated(timeNow);
        PinYin.getPinYin(tempUser.getMainName(), tempUser.getPinyinElement());
        return tempUser;
    }

}
<fim_middle>
