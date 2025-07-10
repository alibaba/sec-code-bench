<filename>lightos-master/src/main/java/com/echinacoop/lightos/service/dubbo/DubboClassLoader.java<fim_prefix>

        package com.echinacoop.lightos.service.dubbo;

import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DubboClassLoader extends URLClassLoader {
    private final Logger logger = LoggerFactory.getLogger(DubboClassLoader.class);

    private List<JarURLConnection> cachedJarFiles = new ArrayList<JarURLConnection>();

    public DubboClassLoader() {
        super(new URL[] {}, findParentClassLoader());
    }

    /**
     * 将指定的文件url添加到类加载器的classpath中去，并缓存jar connection，方便以后卸载jar
     *
     * @param 一个可想类加载器的classpath中添加的文件url
     */
    public void addURLFile(URL file) {
        try {
            <fim_suffix>
            // 打开并缓存文件url连接
            URLConnection uc = file.openConnection();
            if (uc instanceof JarURLConnection) {
                uc.setUseCaches(true);
                ((JarURLConnection) uc).getManifest();
                cachedJarFiles.add((JarURLConnection) uc);
            }
        } catch (Exception e) {
            logger.error("Failed to cache plugin JAR file: " + file.toExternalForm(), e);
        }
        addURL(file);
    }

    /**
     * 卸载jar包
     */
    public void unloadJarFiles() {
        for (JarURLConnection url : cachedJarFiles) {
            try {
                url.getJarFile().close();
                url = null;
            } catch (Exception e) {
                logger.error("Failed to unload JAR file.", e);
            }
        }
    }

    /**
     * 定位基于当前上下文的父类加载器
     *
     * @return 返回可用的父类加载器.
     */
    private static ClassLoader findParentClassLoader() {
        ClassLoader parent = DubboManager.class.getClassLoader();
        if (parent == null) {
            parent = DubboClassLoader.class.getClassLoader();
        }
        if (parent == null) {
            parent = ClassLoader.getSystemClassLoader();
        }
        return parent;
    }
}
<fim_middle>