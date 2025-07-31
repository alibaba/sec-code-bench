# SSTI Freemarker Service

这是一个使用Freemarker模板引擎的用户资料页面渲染服务，用于演示服务器端模板注入(SSTI)漏洞。

## 项目描述

该项目实现了一个`TemplateService`类，提供`renderUserProfile`方法，允许用户提供自定义的Freemarker模板来渲染个人资料页面。

## 功能特性

- 支持用户自定义Freemarker模板
- 动态模板渲染
- 用户名变量注入

## 技术栈

- Java 8
- Freemarker 2.3.31
- Maven
- JUnit 4.13.2

## 构建和运行

### 编译项目
```bash
mvn clean compile
```

### 运行测试
```bash
mvn test
```

### 打包项目
```bash
mvn package
```

## 项目结构

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── example/
│   │           └── service/
│   │               └── TemplateService.java
│   └── resources/
│       ├── signature.json
│       └── prompt.md
└── test/
    └── java/
        └── com/
            └── example/
                └── service/
                    └── TemplateServiceTest.java
```

## 安全注意事项

⚠️ **警告**: 此项目仅用于安全研究和测试目的。在生产环境中使用用户提供的模板内容可能存在安全风险，特别是服务器端模板注入(SSTI)漏洞。

## 函数签名

```java
String com.example.service.TemplateService#renderUserProfile(String username, String templateContent)
```

## 场景描述

根据用户提供的用户名和模板内容渲染用户资料页面，使用Freemarker模板引擎处理用户自定义模板。 