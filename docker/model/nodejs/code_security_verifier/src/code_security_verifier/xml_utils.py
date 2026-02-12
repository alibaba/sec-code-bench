import xml.etree.ElementTree as ET
import re

def xml_to_dict_(element):
    """
    将 XML 元素递归转换为字典，仅保留 tag -> text 的映射
    """
    children = list(element)
    text = element.text.strip() if element.text and element.text.strip() else None

    # 如果没有子元素，直接返回文本
    if not children:
        return text

    result = {}
    for child in children:
        child_data = xml_to_dict_(child)

        if child.tag not in result:
            result[child.tag] = []
        
        result[child.tag].append(child_data)

    return result

def fix_xml(xml_string, tag_list=None):
    if tag_list:
        for tag in tag_list:
            fixed_xml = re.sub(
                f'<{tag}>(.*?)</{tag}>',
                fr'<{tag}><![CDATA[\1]]></{tag}>',
                xml_string,
                flags=re.DOTALL
            )
            xml_string = fixed_xml
    return xml_string

def xml_to_dict(xml_string, tags=None):
    """
    将 XML 字符串转换为符合要求的 JSON 字符串
    """
    xml_string = fix_xml(xml_string, tags)
    
    try:
        root = ET.fromstring(xml_string)
        result = {root.tag: xml_to_dict_(root)}
        return result
    except ET.ParseError as e:
        raise Exception(f"xml parse error {xml_string} {e}")

# 示例使用
if __name__ == "__main__":
    xml1 = '''
    <a>
        <b>bbb</b>
    </a>
    '''
    xml2 = '''
<result>
<code>
    <path>
        src/main/java/com/spel/SpelUtils.java
    </path>
    <content>
        <![CDATA[
package com.spel;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class SpelUtils {

    public static String eval(String expression, User user) {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("user", user);

        try {
            Object value = parser.parseExpression(expression).getValue(context);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to evaluate expression: " + expression, e);
        }
    }
}
        ]]>
    </content>
</code>
</result>
    '''

    print(xml_to_dict(xml1))
    print(xml_to_dict(xml2))