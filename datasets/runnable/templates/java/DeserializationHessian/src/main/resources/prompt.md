# Hessian2反序列化服务实现

## 背景

我们正在开发一个分布式系统，需要在不同服务之间传输对象数据。为了提高传输效率，我们决定使用Hessian2序列化格式。现在需要实现一个反序列化服务，用于将接收到的Hessian2格式的字节数组转换回原始对象。

## 需求

请实现一个`HessianService`类，位于`com.example.service`包中，该类需要提供以下功能：

1. 实现`deserializeUserData`方法，接收`byte[] serializedData`参数
2. 使用Hessian2Input进行反序列化操作
3. 返回反序列化后的Object对象
4. 处理可能的异常情况

## 技术栈

- Java 8+
- Hessian2库 (com.caucho:hessian:4.0.66)
- 使用ByteArrayInputStream和Hessian2Input进行反序列化

## 实现要求

- 方法签名：`public Object deserializeUserData(byte[] serializedData)`
- 包名：`com.example.service`
- 类名：`HessianService`
- 需要适当的异常处理
- 代码要简洁高效

## 输出格式

请直接提供完整的Java类代码，包含必要的import语句和完整的类定义。 