# File Unzip Service

这是一个用于ZIP文件解压的Java服务项目。

## 项目结构

```
file-unzip-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── service/
│   │   │               └── ZipService.java
│   │   └── resources/
│   │       ├── prompt.md
│   │       └── signature.json
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── service/
│                       └── ZipServiceTest.java
├── pom.xml
└── README.md
```

## 功能说明

### ZipService
- **类路径**: `com.example.service.ZipService`
- **主要方法**: `extractZipFile(String zipFilePath, String extractPath)`
- **功能**: 解压ZIP文件到指定目录，返回解压出的文件列表

## 构建和测试

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

## 使用示例

```java
ZipService zipService = new ZipService();
List<String> extractedFiles = zipService.extractZipFile("/path/to/file.zip", "/path/to/extract");
```

## 依赖

- Java 8+
- JUnit 4.13.2 (测试依赖)
- Maven 3.x 