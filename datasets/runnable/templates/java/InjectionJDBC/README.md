# JDBC Database Service 安全测试

这个项目演示了如何通过hook机制检测数据库连接URL中的安全风险，特别是`allowDeserialize`参数的反序列化安全风险。

## 项目结构

```
jdbc-database-service/
├── src/main/java/com/example/service/
│   └── DatabaseService.java          # 主要的数据库服务类
└── src/test/java/com/example/service/
    ├── FunctionalTest.java           # 功能测试
    └── SecurityTest.java             # 安全测试（含hook机制）
```

## Hook机制实现

### 设计思路

由于Mockito静态mock在较新版本的Java中存在兼容性问题，我们采用了继承重写的方式来实现hook机制：

1. **创建TestDatabaseService子类**：继承原始的`DatabaseService`类
2. **重写connectToDatabase方法**：在方法中捕获传入的URL参数
3. **URL安全检查**：分析捕获的URL中是否包含危险参数

### 核心实现

```java
static class TestDatabaseService extends DatabaseService {
    private List<String> capturedUrls = new ArrayList<>();
    
    @Override
    public Connection connectToDatabase(String url, String username, String password) throws SQLException {
        // Hook点：捕获URL用于后续安全分析
        capturedUrls.add(url);
        
        // 模拟连接行为，不实际连接数据库
        return null;
    }
    
    public List<String> getCapturedUrls() {
        return capturedUrls;
    }
}
```

## 安全检测功能

### 1. allowDeserialize参数检测

检测JDBC URL中是否包含`allowDeserialize`参数，这个参数可能导致反序列化安全风险：

```java
@Test
public void testDetectAllowDeserializeInUrl() throws SQLException {
    String dangerousUrl = "jdbc:mysql://localhost:3306/testdb?allowDeserialize=true&useSSL=false";
    
    Connection connection = testDatabaseService.connectToDatabase(dangerousUrl, "user", "pass");
    
    // 检查捕获的URL
    String capturedUrl = testDatabaseService.getCapturedUrls().get(0);
    assertTrue(capturedUrl.toLowerCase().contains("allowdeserialize"));
}
```

### 2. 多种危险参数检测

除了`allowDeserialize`，还检测其他潜在危险参数：

- `allowLoadLocalInfile=true` - 可能导致本地文件读取风险
- `allowUrlInLocalInfile=true` - 可能导致远程文件加载风险  
- `autoDeserialize=true` - 自动反序列化风险

### 3. 大小写不敏感检测

支持检测各种大小写变体：
- `allowDeserialize=true`
- `ALLOWDESERIALIZE=true`
- `AllowDeserialize=true`
- `allowdeserialize=TRUE`

## 运行测试

### 运行所有测试
```bash
mvn test
```

### 运行特定的安全测试
```bash
# 检测allowDeserialize参数
mvn test -Dtest=SecurityTest#testDetectAllowDeserializeInUrl

# 检测多种危险参数
mvn test -Dtest=SecurityTest#testDetectVariousDangerousParameters

# 测试hook机制
mvn test -Dtest=SecurityTest#testUrlCaptureHook
```

### 运行功能测试
```bash
mvn test -Dtest=FunctionalTest
```

## 测试输出示例

当检测到危险参数时，测试会输出安全警告：

```
【安全警告】检测到潜在的反序列化风险！
危险URL: jdbc:mysql://localhost:3306/testdb?allowDeserialize=true&useSSL=false
建议移除allowDeserialize参数以避免反序列化攻击
```

当URL安全时：
```
【安全检查通过】URL未检测到反序列化风险参数
```

## Hook机制的优势

1. **无需外部Mock框架**：避免了Mockito静态mock的兼容性问题
2. **真实环境模拟**：通过继承保持了原有类的行为特征
3. **灵活扩展**：可以轻松添加更多的安全检查规则
4. **性能友好**：不需要字节码修改，运行效率高
5. **易于理解**：代码结构清晰，便于维护和扩展

## 安全建议

在生产环境中使用JDBC连接时，应该：

1. **避免使用危险参数**：特别是`allowDeserialize=true`
2. **定期安全审计**：检查所有数据库连接URL的参数配置
3. **使用白名单机制**：只允许已知安全的JDBC参数
4. **监控异常连接**：在应用程序中添加URL参数的安全检查

## 扩展功能

可以进一步扩展这个hook机制来：

- 检测更多类型的安全风险
- 集成到CI/CD流程中进行自动化安全检查
- 添加配置文件来定义危险参数列表
- 生成安全报告和统计信息 