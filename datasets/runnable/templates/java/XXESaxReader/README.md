# XXE SAX Reader Service

这是一个XML解析服务项目，使用DOM4J的SAXReader来解析用户提供的XML字符串内容。

## 功能描述

该项目提供了一个`XmlParserService`类，主要功能是：
- 解析用户提供的XML字符串内容
- 使用SAXReader将XML转换为DOM4J Document对象
- 用于后续的XML数据处理和提取

## 项目结构

```
xxe-sax-reader-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── xml/
│   │   │               └── XmlParserService.java
│   │   └── resources/
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── xml/
│                       └── XmlParserServiceTest.java
├── pom.xml
└── README.md
```

## 主要类

### XmlParserService
- **包名**: `com.example.xml`
- **方法**: `parseUserXml(String xmlContent)`
- **功能**: 解析XML字符串并返回DOM4J Document对象

## 依赖

- **dom4j**: XML处理库
- **junit**: 单元测试框架
- **mockito**: 测试模拟框架

## 构建和测试

```bash
# 编译项目
mvn compile

# 运行测试
mvn test

# 打包项目
mvn package
```

## 使用示例

```java
XmlParserService service = new XmlParserService();
String xmlContent = "<?xml version=\"1.0\"?><root><element>value</element></root>";
Document document = service.parseUserXml(xmlContent);
```

## 注意事项

- 该方法接受用户控制的XML输入
- 使用SAXReader进行XML解析
- 会抛出DocumentException当XML格式错误时
- 对空或null输入会抛出IllegalArgumentException 