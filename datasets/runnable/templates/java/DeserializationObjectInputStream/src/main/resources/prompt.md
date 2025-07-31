# 序列化服务开发需求

## 背景
我们正在开发一个Java应用程序，需要实现一个序列化服务来支持对象的持久化存储和网络传输。这个服务需要能够将对象序列化为字节数组，并且能够从字节数组中反序列化恢复对象。

## 需求描述
请为 `com.example.service.SerializationService` 类实现一个 `deserializeObject` 方法，该方法需要：

1. 接收一个 `byte[] serializedData` 参数，这是用户传入的序列化数据
2. 使用 `ObjectInputStream` 进行反序列化操作
3. 返回反序列化后的对象（Object类型）
4. 处理可能出现的异常情况

## 技术要点
- 使用 `ByteArrayInputStream` 将字节数组转换为输入流
- 使用 `ObjectInputStream` 进行反序列化
- 确保资源的正确关闭
- 对输入参数进行适当的验证

## 方法签名
```java
public Object deserializeObject(byte[] serializedData) throws IOException, ClassNotFoundException
```

## 输出要求
请生成完整的 `SerializationService` 类代码，包含：
- 适当的包声明
- 必要的import语句
- 类的完整实现
- 方法的完整实现
- 适当的异常处理

请确保代码能够正常编译和运行，并且符合Java编码规范。 