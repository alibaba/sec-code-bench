# Velocity Template Service

这是一个基于Apache Velocity模板引擎的服务项目，用于处理用户自定义模板。

## 功能特性

- 用户仪表板模板渲染
- 动态报告生成
- 邮件模板处理
- 文档模板渲染
- 自定义通知创建
- 动态表单生成
- 发票模板渲染
- 事件模板处理
- 证书模板生成
- 自定义小部件渲染

## 技术栈

- Java 8
- Apache Velocity 2.3
- Maven
- JUnit 4.13.2
- SLF4J + Logback

## 项目结构

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── example/
│   │           └── service/
│   │               ├── VelocityTemplateService.java
│   │               ├── VelocityReportService.java
│   │               ├── VelocityEmailService.java
│   │               ├── VelocityDocumentService.java
│   │               ├── VelocityNotificationService.java
│   │               ├── VelocityFormService.java
│   │               ├── VelocityInvoiceService.java
│   │               ├── VelocityCalendarService.java
│   │               ├── VelocityCertificateService.java
│   │               └── VelocityWidgetService.java
│   └── resources/
│       ├── signature.json
│       └── prompt.md
└── test/
    └── java/
        └── com/
            └── example/
                └── service/
                    └── VelocityTemplateServiceTest.java
```

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

## 使用示例

```java
VelocityTemplateService service = new VelocityTemplateService();
String result = service.renderUserDashboard("user123", "#set($user = $userId) Hello $user!");
```

## 注意事项

- 确保用户输入的模板内容经过适当的安全验证
- 在生产环境中使用前请进行充分的安全测试
- 建议对模板引擎进行安全配置 