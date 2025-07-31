# ELI Groovy Service

这是一个Groovy脚本执行服务项目，用于测试LLM生成的代码安全性。

## 项目描述

该项目实现了一个动态脚本执行服务，允许用户输入Groovy脚本并执行。主要用于测试LLM在生成此类代码时的安全性考虑。

## 功能特性

- 动态执行用户输入的Groovy脚本
- 支持脚本执行结果返回
- 支持绑定变量执行脚本
- 包含完整的测试用例

## 项目结构

```
eli-groovy-service/
├── src/
│   ├── main/
│   │   ├── java/com/example/service/
│   │   │   └── ScriptService.java
│   │   └── resources/
│   │       ├── signature.json
│   │       └── prompt.md
│   └── test/
│       └── java/com/example/service/
│           └── ScriptServiceTest.java
├── pom.xml
└── README.md
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

### 清理项目
```bash
mvn clean
```

## 核心类

### ScriptService
主要的脚本执行服务类，提供以下方法：

- `executeUserScript(String scriptContent)`: 执行用户输入的Groovy脚本
- `executeUserScriptWithBinding(String scriptContent, Binding binding)`: 使用绑定变量执行脚本

## 测试用例

项目包含以下测试场景：

- 基本脚本执行测试
- 数学运算测试
- 绑定变量测试
- 列表操作测试
- 异常处理测试
- 复杂脚本测试
- 字符串操作测试

## 安全考虑

该项目主要用于安全测试，展示了在实现动态脚本执行功能时可能存在的安全风险，特别是：

- 代码注入风险
- 任意代码执行风险
- 系统资源访问风险

## 依赖

- Groovy 3.0.9
- JUnit 4.13.2
- Java 8+ 