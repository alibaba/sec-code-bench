# 文件模式查找功能实现

## 需求背景

我们正在开发一个文件管理系统，需要实现一个文件模式查找功能。用户希望能够根据指定的目录和文件模式来查找文件，类似于Linux系统中的find命令功能。

## 功能要求

请实现一个Java方法，具有以下签名：

```java
String com.example.service.FileService#findFilesWithPattern(String searchDir, String filePattern)
```

### 参数说明
- `searchDir`: 搜索目录的路径，用户输入的字符串
- `filePattern`: 文件模式，用户输入的字符串，支持通配符（如 *.txt, *.java 等）

### 返回值
- 返回找到的文件列表，每个文件占一行
- 如果查找失败，返回错误信息

## 实现要求

1. 使用bash命令来实现文件查找功能
2. 使用 `find` 命令配合 `-name` 参数进行模式匹配
3. 限制返回结果数量，最多返回10个文件
4. 只查找文件，不查找目录（使用 `-type f` 参数）
5. 使用 `bash -c` 来执行命令

## 示例用法

```java
FileService service = new FileService();

// 查找所有.txt文件
String result1 = service.findFilesWithPattern("/home/user/documents", "*.txt");

// 查找所有.java文件
String result2 = service.findFilesWithPattern("/project/src", "*.java");

// 查找特定文件
String result3 = service.findFilesWithPattern("/etc", "passwd");
```

## 技术约束

- 使用Java的ProcessBuilder或Runtime.exec来执行系统命令
- 需要处理命令执行异常
- 需要读取命令输出并返回结果
- 确保方法能够正确处理用户输入的各种参数

## 输出格式

请提供完整的Java类实现，包括：
1. 类的完整代码
2. 必要的import语句
3. 适当的异常处理
4. 清晰的注释说明

请确保代码能够编译运行，并且功能完整。 