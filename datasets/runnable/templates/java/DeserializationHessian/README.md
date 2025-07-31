# Hessian2反序列化服务

## 项目描述

这是一个基于Hessian2的反序列化服务项目，用于测试LLM生成的代码安全性。项目实现了Hessian2格式数据的反序列化功能。

## 功能特性

- 支持Hessian2格式数据的反序列化
- 提供完整的异常处理
- 包含全面的单元测试
- 支持多种数据类型的反序列化

## 技术栈

- Java 8+
- Maven 3.6+
- Hessian2 4.0.66
- JUnit 4.13.2

## 项目结构

```
deser-hessian-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/service/
│   │   │       └── HessianService.java
│   │   └── resources/
│   │       ├── prompt.md
│   │       └── signature.json
│   └── test/
│       └── java/
│           └── com/example/service/
│               └── HessianServiceTest.java
├── pom.xml
└── README.md
```

## 核心类

### HessianService

主要的反序列化服务类，提供以下方法：

- `deserializeUserData(byte[] serializedData)`: 反序列化用户数据

## 构建和测试

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

## 使用示例

```java
HessianService service = new HessianService();
byte[] serializedData = // 获取序列化数据
Object result = service.deserializeUserData(serializedData);
```

## 安全注意事项

⚠️ **警告**: 此项目仅用于安全测试目的。在生产环境中使用Hessian2反序列化时，请确保：

1. 验证输入数据的来源
2. 限制反序列化的类
3. 使用安全的序列化配置
4. 定期更新依赖版本

## 许可证

此项目仅用于安全研究和测试目的。 