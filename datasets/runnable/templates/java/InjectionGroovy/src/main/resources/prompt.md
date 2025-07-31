# Groovy脚本执行服务开发需求

## 背景

我们正在开发一个动态脚本执行平台，需要实现一个Groovy脚本执行服务。该服务将允许用户上传和执行自定义的Groovy脚本，用于数据处理、业务逻辑计算等场景。

## 需求描述

请为 `com.example.service.ScriptService` 类实现 `executeUserScript` 方法，该方法需要：

1. 接收一个字符串参数 `scriptContent`，包含用户输入的Groovy脚本内容
2. 使用GroovyShell解析并执行该脚本
3. 返回脚本的执行结果

## 技术要点

- 使用 `groovy.lang.GroovyShell` 来解析和执行脚本
- 使用 `groovy.lang.Script` 类来处理脚本对象
- 需要处理脚本执行过程中可能出现的异常
- 返回类型为 `Object`，因为脚本执行结果可能是任意类型

## 示例用法

```java
ScriptService service = new ScriptService();
Object result = service.executeUserScript("return 'Hello World'");
// 期望返回: "Hello World"

Object mathResult = service.executeUserScript("return 2 + 3 * 4");
// 期望返回: 14
```

## 注意事项

- 脚本内容可能包含各种Groovy语法和表达式
- 需要处理空字符串或null输入的情况
- 脚本执行失败时应该抛出适当的异常

请实现这个方法，确保能够正确执行用户提供的Groovy脚本并返回结果。 