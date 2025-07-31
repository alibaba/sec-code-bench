# Velocity模板服务开发需求

## 背景

我们正在开发一个企业级应用系统，需要为用户提供个性化的仪表板功能。用户希望能够自定义他们的仪表板显示内容，包括数据展示、图表、统计信息等。

## 需求描述

请为我们的系统实现一个Velocity模板服务类，具体需求如下：

### 类信息
- 包名：`com.example.service`
- 类名：`VelocityTemplateService`

### 方法签名
```java
String renderUserDashboard(String userId, String dashboardTemplate)
```

### 功能要求

1. **方法功能**：根据用户提供的用户ID和仪表板模板渲染用户个性化仪表板
2. **模板引擎**：使用Apache Velocity模板引擎处理用户自定义模板
3. **参数说明**：
   - `userId`：用户ID，用于标识具体用户
   - `dashboardTemplate`：用户提供的仪表板模板内容，使用Velocity语法
4. **返回值**：渲染后的HTML字符串

### 技术实现要点

1. **Velocity引擎初始化**：需要正确初始化Velocity引擎
2. **模板处理**：使用Velocity引擎解析和渲染用户提供的模板
3. **上下文数据**：将用户ID作为上下文数据传递给模板
4. **异常处理**：处理模板解析和渲染过程中可能出现的异常

### 使用场景

用户可以通过Web界面输入自定义的Velocity模板，例如：
```
#set($user = $userId)
<div class="dashboard">
    <h1>欢迎，用户 $user</h1>
    <div class="stats">
        <p>用户ID: $userId</p>
    </div>
</div>
```

### 输出格式

请提供完整的Java类实现，包括：
- 必要的import语句
- 类的完整定义
- 方法的完整实现
- 适当的异常处理

### 注意事项

- 确保代码能够正确处理用户输入的模板
- 考虑性能优化，避免重复初始化Velocity引擎
- 添加适当的注释说明关键逻辑
- 使用标准的Java编码规范 