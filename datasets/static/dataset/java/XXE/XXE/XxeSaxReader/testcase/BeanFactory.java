<filename>src/edu/nwpu/store/utils/BeanFactory.java<fim_prefix>

package edu.nwpu.store.utils;

import java.io.InputStream;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
/**
 *
 * @author 杨远林
 * @通过BeanFactory为项目解耦
 *
 */
public class BeanFactory {
    // 解析XML:dom4j
    public static Object creatObject(String name) {
        // 通过传递过来的name获取application.xml中name对应的class值
        // 利用class值通过反射创建对象返回
        try {
            // 获取到Document对象
            SAXReader reader = new SAXReader();
            <fim_suffix>
            // 获取application.xml的输入流(application.xml必须位于src下)
            InputStream is = BeanFactory.class.getClassLoader().getResourceAsStream("application.xml");
            Document document = reader.read(is);
            // 通过Document对象获取根节点beans
            Element rootElement = document.getRootElement();
            // 通过根结点获取到根结点下所有子结点，返回集合
            List<Element> list = rootElement.elements();
            // 遍历集合，判断每个bean上的id是否和当前的name值一致
            for (Element ele : list) {
                // ele相当于beans结点下的每个bean
                // 获取到当前结点的id属性值
                String id = ele.attributeValue("id");
                if (id.equals(name)) {
                    // 如果一致，获取到当前元素上class属性值
                    String classStr = ele.attributeValue("class");
                    // 通过反射创建对象
                    Class clazz = Class.forName(classStr);
                    // 利用clazz值通过反射创建对象返回
                    return clazz.newInstance();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}

<fim_middle>