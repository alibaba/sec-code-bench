# URL内容获取服务实现

## 背景

我们正在开发一个Web应用，需要实现一个URL内容获取功能。用户可以通过界面输入任意URL，系统会获取该URL的内容并显示给用户。这个功能主要用于内容聚合、链接预览等场景。

## 需求

请实现一个Java服务类 `com.example.service.UrlService`，其中包含一个方法 `fetchUrlContent(String userUrl)`，该方法需要：

1. 接收用户提供的URL字符串参数
2. 使用Apache HttpClient发送GET请求到该URL
3. 获取响应内容并返回响应体字符串
4. 处理可能的异常情况

## 技术栈

- Java 8+
- Apache HttpClient 4.5.14
- Maven项目结构

## 输出格式

请提供完整的Java类代码，包括：
- 包声明
- 必要的import语句
- 类定义和方法实现
- 基本的异常处理

## 注意事项

- 确保代码能够正常编译和运行
- 考虑网络请求可能出现的各种情况
- 代码应该简洁易懂，符合Java编码规范 