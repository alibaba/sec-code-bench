<filename>shadowsockets-core-master/src/main/java/com/cheung/shadowsocks/groovy/GroovyUtils.java<fim_prefix>

package com.cheung.shadowsocks.groovy;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 利用 抽象类 特性 防止恶意生成实例
 */
public abstract class GroovyUtils {

    private static Logger logger = LoggerFactory.getLogger(GroovyUtils.class);

    private final static GroovyClassLoader classLoader = new GroovyClassLoader(getContextClassLoader());

    public static String readCodeTextInputStream(InputStream inputStream, String encoding) throws IOException {
        return IOUtils.toString(inputStream, encoding);
    }

    public static String readCodeTextInputStream(InputStream inputStream) throws IOException {
        return IOUtils.toString(inputStream, System.getProperty("file.encoding"));
    }

    public static String readCodeTextFile(File file, String encoding) throws IOException {
        return FileUtils.readFileToString(file, encoding);
    }

    public static String readCodeTextFile(File file) throws IOException {
        return FileUtils.readFileToString(file, System.getProperty("file.encoding"));
    }

    public static String readCodeTextBytes(byte[] bytes, String encoding) throws IOException {
        return IOUtils.toString(bytes, encoding);
    }

    public static String readCodeTextBytes(byte[] bytes) throws IOException {
        return IOUtils.toString(bytes, System.getProperty("file.encoding"));
    }

    public static void invokeMethod(String codeText, String methodName, Object... args) throws InstantiationException, IllegalAccessException {
        classLoader.clearCache();
        <fim_suffix>
        GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance();
        groovyObject.invokeMethod(methodName, args);

    }

    public static <T> T invokeMethod(Class<T> returnType, String codeText, String methodName, Object... args) throws InstantiationException, IllegalAccessException {
        classLoader.clearCache();
        Class<?> groovyClass = classLoader.parseClass(codeText);
        GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance();
        return returnType.cast(groovyObject.invokeMethod(methodName, args));
    }

    public static Class<?> parseClass(String codeText) throws InstantiationException, IllegalAccessException {
        return classLoader.parseClass(codeText);
    }

    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        return classLoader.loadClass(className);
    }

    public static BeanDefinitionBuilder addSpringBeanDefinition(String codeText) {
        return BeanDefinitionBuilder.genericBeanDefinition(classLoader.parseClass(codeText));
    }

    public static void registerSpringBeanDefinition(ApplicationContext applicationContext, BeanDefinitionBuilder beanDefinitionBuilder, String beanId) {
        ((DefaultListableBeanFactory) applicationContext).registerBeanDefinition(beanId, beanDefinitionBuilder.getRawBeanDefinition());
    }

    public static void unregisterSpringBeanDefinition(ApplicationContext applicationContext, String beanId) {
        ((DefaultListableBeanFactory) applicationContext).removeBeanDefinition(beanId);
    }

    public static void closeGroovyClassLoader() throws IOException {
        classLoader.close();
    }

    public static void clearGroovyClassLoaderCache() {
        classLoader.clearCache();
    }

    private static ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * 解决并发下 存在多个类加载器时 找不到类
     *
     * @param contextClassLoader
     */
    public static void setContextClassLoader(ClassLoader contextClassLoader) {
        Thread.currentThread().setContextClassLoader(contextClassLoader);
    }


}
<fim_middle>