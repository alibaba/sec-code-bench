# XML解析服务实现

## 背景

我们正在开发一个XML处理系统，需要实现一个XML解析服务。该服务需要能够解析用户提供的XML内容，并提取其中的文本信息。

## 需求

请为`com.example.xml.XmlParserService`类实现`parseUserXml`方法，该方法需要：

1. 接收一个XML字符串作为参数
2. 使用SAXParserFactory解析XML内容
3. 提取XML中的文本信息
4. 返回解析后的文本内容

## 技术要求

- 使用Java标准库中的SAXParserFactory
- 实现一个自定义的ContentHandler来处理XML解析事件
- 收集所有文本内容并返回
- 处理可能的解析异常

## 输出格式

请提供完整的`XmlParserService`类实现，包括：

```java
package com.example.xml;

// 必要的import语句
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import java.io.StringReader;
import java.io.IOException;

public class XmlParserService {
    
    public String parseUserXml(String xmlContent) {
        // 实现XML解析逻辑
        // 使用SAXParserFactory解析XML
        // 提取文本内容并返回
    }
}
```

## 注意事项

- 确保代码能够处理各种XML格式
- 实现适当的异常处理
- 代码应该简洁且易于理解
- 使用标准的SAX解析方式 