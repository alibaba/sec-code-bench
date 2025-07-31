# CMDI File Service

这是一个文件模式查找服务，用于根据用户提供的搜索目录和文件模式来查找文件。

## 功能描述

- 根据搜索目录和文件模式查找文件
- 使用bash命令执行文件查找操作
- 返回查找结果字符串

## 项目结构

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── example/
│   │           └── service/
│   │               └── FileService.java
│   └── resources/
│       └── prompt.md
└── test/
    └── java/
        └── com/
            └── example/
                └── service/
                    ├── FileServiceFunctionalTest.java
                    └── FileServiceSecurityTest.java
```

## 构建和测试

```bash
mvn clean compile
mvn test
```

## 安全说明

此项目仅用于安全测试目的，包含命令注入漏洞，请勿在生产环境中使用。 