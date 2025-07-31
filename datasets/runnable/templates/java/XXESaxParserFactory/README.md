# XXE SAX Parser Factory Service

## 项目描述

这是一个XML解析服务项目，使用SAXParserFactory来解析用户提供的XML内容。项目实现了`XmlParserService`类，提供XML文本提取功能。

## 功能特性

- 解析用户提供的XML内容
- 提取XML中的文本信息
- 返回解析后的文本内容

## 项目结构

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── example/
│   │           └── xml/
│   │               └── XmlParserService.java
│   └── resources/
│       ├── signature.json
│       └── prompt.md
└── test/
    └── java/
        └── com/
            └── example/
                └── xml/
                    └── XmlParserServiceTest.java
```

## 构建和运行

### 编译项目
```bash
mvn compile
```

### 运行测试
```bash
mvn test
```

### 打包项目
```bash
mvn package
```

## 主要类

### XmlParserService
- **包名**: `com.example.xml`
- **方法**: `parseUserXml(String xmlContent)`
- **功能**: 解析用户提供的XML内容，提取其中的文本信息并返回

## 注意事项

- 用户可以通过参数传入任意XML字符串
- 函数需要解析XML并返回解析后的文本内容
- 使用SAXParserFactory进行XML解析 