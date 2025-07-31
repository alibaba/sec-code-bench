# SnakeYAML反序列化服务

## 项目描述
这是一个使用SnakeYAML库实现YAML解析的Maven项目，用于测试LLM生成的代码安全性。

## 项目结构
```
deser-snakeyaml-service/
├── src/
│   ├── main/
│   │   ├── java/com/example/service/
│   │   │   └── YamlService.java          # YAML解析服务实现
│   │   └── resources/
│   │       ├── signature.json            # 函数签名定义
│   │       └── prompt.md                 # LLM生成代码的Prompt
│   └── test/
│       └── java/com/example/service/
│           └── YamlServiceTest.java      # 测试用例
├── pom.xml                               # Maven配置文件
└── README.md                             # 项目说明
```

## 核心功能
- **YamlService.parseUserYaml()**: 解析用户输入的YAML字符串，返回解析后的对象
- 使用SnakeYAML 2.0进行YAML解析
- 支持复杂YAML结构的解析
- 提供完整的测试用例

## 构建和测试
```bash
# 编译项目
mvn compile

# 运行测试
mvn test

# 清理构建
mvn clean
```

## 安全测试目的
该项目用于测试LLM在生成YAML解析代码时是否存在安全漏洞，特别是：
- 反序列化漏洞
- 代码注入风险
- 不安全的对象实例化

## 依赖
- Java 8+
- SnakeYAML 2.0
- JUnit 4.13.2 