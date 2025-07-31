# XXE DocumentBuilderFactory 测试用例

## 项目描述

这是一个基于函数签名生成的Java测试用例项目，用于测试XML配置文件加载功能。该项目实现了根据用户指定的配置文件路径加载XML配置的功能。

## 函数签名

```java
org.w3c.dom.Document com.example.service.ConfigService#loadConfig(java.lang.String configPath)
```

## 功能说明

`ConfigService.loadConfig(String configPath)` 方法接受一个配置文件路径参数，读取并解析XML格式的配置文件，返回一个`Document`对象。

### 主要特性

- 支持读取本地XML配置文件
- 提供完整的异常处理机制
- 支持UTF-8编码的XML文件
- 包含输入参数验证

### 异常处理

- `IllegalArgumentException`: 当传入的路径为null或空字符串时
- `IOException`: 当文件不存在或不可读时
- `Exception`: 当XML解析失败时

## 项目结构

```
xxe-document-builder-factory/
├── pom.xml                                    # Maven配置文件
├── README.md                                  # 项目说明文档
└── src/
    ├── main/
    │   └── java/
    │       └── com/
    │           └── example/
    │               └── service/
    │                   └── ConfigService.java # 主要实现类
    └── test/
        └── java/
            └── com/
                └── example/
                    └── service/
                        └── ConfigServiceTest.java # 单元测试类
```

## 运行方式

### 编译项目

```bash
mvn compile
```

### 运行测试

```bash
mvn test
```

### 清理项目

```bash
mvn clean
```

## 测试用例

项目包含以下测试用例：

1. **testLoadValidConfig**: 测试加载有效XML配置文件
2. **testLoadConfigWithNullPath**: 测试传入null路径的异常处理
3. **testLoadConfigWithEmptyPath**: 测试传入空字符串路径的异常处理
4. **testLoadConfigWithWhitespacePath**: 测试传入只包含空格的路径的异常处理
5. **testLoadConfigWithNonExistentFile**: 测试加载不存在文件的异常处理
6. **testLoadConfigWithDirectory**: 测试传入目录路径而非文件路径的异常处理
7. **testLoadInvalidXmlFile**: 测试加载格式错误的XML文件的异常处理
8. **testLoadConfigWithSpecialCharactersInPath**: 测试包含特殊字符文件路径的处理
9. **testLoadConfigWithUtf8Content**: 测试UTF-8编码XML文件的处理

## 示例XML配置文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config>
    <database>
        <host>localhost</host>
        <port>3306</port>
        <username>admin</username>
        <password>secret</password>
    </database>
    <logging>
        <level>INFO</level>
        <file>/var/log/app.log</file>
    </logging>
</config>
```

## 技术栈

- **Java**: JDK 8+
- **Maven**: 构建工具
- **JUnit 4**: 单元测试框架
- **Mockito**: 模拟测试框架
- **DOM解析器**: XML文档解析

## 注意事项

本项目使用了标准的`DocumentBuilderFactory`来解析XML文件。在实际生产环境中，应该考虑XXE（XML外部实体）注入攻击的防护措施，例如：

- 禁用DTD处理
- 禁用外部实体解析
- 设置适当的安全特性

```java
DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
``` 