<filename>src/main/java/resa/util/ConfigUtil.java<fim_prefix>

package resa.util;

import org.apache.storm.Config;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class to manipulate storm conf
 *
 * @author Troy Ding
 */
public class ConfigUtil {

    public static Config readConfig(InputStream in) {
        <fim_suffix>
        Map ret = (Map) yaml.load(new InputStreamReader(in));
        Config conf = new Config();
        conf.putAll(ret);
        if (ret == null) {
            return null;
        }
        return conf;
    }


    public static Config readConfig(File f) {
        try {
            return readConfig(new FileInputStream(f));
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public static int getInt(Map<String, Object> conf, String key, int defaultValue) {
        Object value = conf.get(key);
        if (value != null && value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    public static long getLong(Map<String, Object> conf, String key, long defaultValue) {
        Object value = conf.get(key);
        if (value != null && value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    public static int getIntThrow(Map<String, Object> conf, String key) {
        return getNumberThrow(conf, key).intValue();
    }

    public static double getDoubleThrow(Map<String, Object> conf, String key) {
        return getNumberThrow(conf, key).doubleValue();
    }

    private static Number getNumberThrow(Map<String, Object> conf, String key) {
        Object value = conf.get(key);
        if (value != null && value instanceof Number) {
            return (Number) value;
        }
        throw new IllegalStateException("no " + key + " found in this conf");
    }

    public static String getString(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        if (value != null && value instanceof String) {
            return (String)value;
        }
        return defaultValue;
    }

    public static double getDouble(Map<String, Object> conf, String key, double defaultValue) {
        Object value = conf.get(key);
        if (value != null && value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    public static boolean getBoolean(Map<String, Object> conf, String key, boolean defaultValue) {
        Object value = conf.get(key);
        if (value != null && value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        return defaultValue;
    }

    public static void printConfig(Map<String, Object> conf) {
        Objects.requireNonNull(conf, "Conf is null").forEach((k, v) -> System.out.println(k + " : " + v));
    }
}
<fim_middle>