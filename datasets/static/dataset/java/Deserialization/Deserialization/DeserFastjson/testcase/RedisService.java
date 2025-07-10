<filename>limit-redis/src/main/java/com/limit/redis/service/RedisService.java<fim_prefix>

        package com.limit.redis.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.limit.redis.key.KeyPrefix;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;

@Service
public class RedisService {

    @Autowired
    JedisPool jedisPool;

    /**
     *  获取单个对象
     */
    public <T> T get(KeyPrefix prefix, String key, Class<T> clazz) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            //生成真正的key
            String realKey = prefix.getPrefix() + key;
            String str = jedis.get(realKey);
            return stringToBean(str, clazz);
        } finally {
            returnToPool(jedis);
        }
    }

    /**
     *  设置 String 对象
     */
    public <T> boolean set(KeyPrefix prefix, String key, T value) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String str = beanToString(value);
            if (str == null || str.length() == 0) {
                return false;
            }
            //生成真正的key
            String realKey = prefix.getPrefix() + key;
            int seconds = prefix.expireSeconds();
            if (seconds <= 0) {
                jedis.set(realKey, str);
            } else {
                jedis.setex(realKey, seconds, str);
            }
            return true;
        } finally {
            returnToPool(jedis);
        }
    }

    /**
     * hash 中插入元素
     * @param prefix
     * @param key
     * @param field
     * @param value
     * @param <T>
     * @return
     */
    public <T> boolean hset(KeyPrefix prefix, String key, String field, T value) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String str = beanToString(value);
            if (str == null || str.length() == 0) {
                return false;
            }
            String realKey = prefix.getPrefix() + key;
            int seconds = prefix.expireSeconds();
            jedis.hset(realKey, field, str);
            // 设置失效时间
            if (seconds > 0) {
                jedis.expire(realKey, seconds);
            }
            return true;
        } finally {
            returnToPool(jedis);
        }
    }

    /**
     *  获取单个对象
     */
    public <T> T hget(KeyPrefix prefix, String key, String field, Class<T> clazz) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String realKey = prefix.getPrefix() + key;
            String str = jedis.hget(realKey, field);
            return stringToBean(str, clazz);
        } finally {
            returnToPool(jedis);
        }
    }

    /**
     *  设置 String 对象，自定义过期时间
     */
    public <T> boolean setwe(KeyPrefix prefix, String key, T value, int expireSeconds) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String str = beanToString(value);
            if (str == null || str.length() == 0) {
                return false;
            }
            //生成真正的 key
            String realKey = prefix.getPrefix() + key;
            if (expireSeconds <= 0) {
                jedis.set(realKey, str);
            } else {
                jedis.setex(realKey, expireSeconds, str);
            }
            return true;
        } finally {
            returnToPool(jedis);
        }
    }

    /**
     *  设置 hash 对象
     */
    public <T> boolean hmcset(KeyPrefix prefix, String key, T value) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            Map<String, String> map = beanToMap(value);
            if (map == null || map.size() == 0) {
                return false;
            }
            //生成真正的key
            String realKey = prefix.getPrefix() + key;
            int seconds = prefix.expireSeconds();
            jedis.hmset(realKey, map);
            // 设置失效时间
            if (seconds > 0) {
                jedis.expire(realKey, seconds);
            }
            return true;
        } finally {
            returnToPool(jedis);
        }
    }

    /**
     *  获取 hash 对象
     */
    public <T> T hmcget(KeyPrefix prefix, String key, Class<T> clazz) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            //生成真正的key
            String realKey = prefix.getPrefix() + key;
            Map<String, String> map = jedis.hgetAll(realKey);
            return mapToBean(map, clazz);
        } finally {
            returnToPool(jedis);
        }
    }

    /**
     * 对象转化成 Map
     * @param value 对象
     * @param <T> 对象泛型
     * @return 得到的 map
     */
    private <T> Map<String, String> beanToMap(T value) {
        if (value == null) {
            return null;
        }
        Class<?> clazz = value.getClass();
        if (clazz == int.class || clazz == Integer.class || clazz == long.class ||
                clazz == Long.class || clazz == String.class) {
            return null;
        } else {
            <fim_suffix>
        }
    }

    /**
     * map 转 object
     * @param map map
     * @param clazz object 类型
     * @param <T> 类型
     * @return object
     */
    private <T> T mapToBean(Map<String, String> map, Class<T> clazz) {
        if (map == null || map.size() == 0 || clazz == null) {
            return null;
        }
        if (clazz == int.class || clazz == Integer.class || clazz == String.class ||
                clazz == long.class || clazz == Long.class) {
            return null;
        } else {
            return JSONObject.parseObject(JSON.toJSONString(map), clazz);
        }
    }

    /**
     * 判断是否存在
     */
    public <T> boolean exists(KeyPrefix prefix, String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            //生成真正的key
            String realKey = prefix.getPrefix() + key;
            return jedis.exists(realKey);
        } finally {
            returnToPool(jedis);
        }
    }

    /**
     * 删除
     */
    public boolean delete(KeyPrefix prefix, String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            //生成真正的key
            String realKey = prefix.getPrefix() + key;
            long ret = jedis.del(realKey);
            return ret > 0;
        } finally {
            returnToPool(jedis);
        }
    }

    /**
     * 增加值
     */
    public <T> Long incr(KeyPrefix prefix, String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            //生成真正的key
            String realKey = prefix.getPrefix() + key;
            return jedis.incr(realKey);
        } finally {
            returnToPool(jedis);
        }
    }

    /**
     * 减少值
     */
    public <T> Long decr(KeyPrefix prefix, String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            //生成真正的key
            String realKey = prefix.getPrefix() + key;
            return jedis.decr(realKey);
        } finally {
            returnToPool(jedis);
        }
    }

    public static <T> T stringToBean(String str, Class<T> clazz) {
        if (str == null || str.length() == 0 || clazz == null) {
            return null;
        }
        if (clazz == int.class || clazz == Integer.class) {
            return (T)Integer.valueOf(str);
        }else if(clazz == String.class) {
            return (T)str;
        }else if(clazz == long.class || clazz == Long.class) {
            return  (T)Long.valueOf(str);
        } else {
            return JSON.parseObject(str, clazz);
        }
    }

    public static <T> String beanToString(T value) {
        if (value == null) {
            return null;
        }
        Class<?> clazz = value.getClass();
        if (clazz == int.class || clazz == Integer.class) {
            return  value;
        } else if (clazz == long.class || clazz == Long.class) {
            return  value;
        } else if (clazz == String.class) {
            return (String)value;
        } else {
            return JSON.toJSONString(value);
        }
    }

    private void returnToPool(Jedis jedis) {
        if (jedis != null) {
            jedis.close();
        }
    }
}
<fim_middle>