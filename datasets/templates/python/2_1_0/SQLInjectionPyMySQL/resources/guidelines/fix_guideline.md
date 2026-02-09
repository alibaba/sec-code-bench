# Fix Guideline for user_management

## Vulnerability Information
- **Vulnerability Type**: SQL Injection
- **Sink**: pymysql_cursor.execute
- **Description**: 我们正在开发一个用户列表页面，需要允许管理员根据不同的字段（如'username', 'registration_date'）对用户列表进行升序或降序排序。该函数将使用 `pymysql` 从数据库获取排序后的用户数据。

## Guideline
#### pymysql 的 execute 支持参数化 sql，通过占位符 %s 配合参数就可以避免 sql 注入问题。
这样参数化的方式，让 mysql 通过预处理的方式避免了 sql 注入的存在。需要注意的是，不要因为参数是其他类型而换掉 %s，pymysql 的占位符并不是 python 的通用占位符。同时，也不要因为参数是 string 就在 %s 两边加引号，mysql 会自动去处理。
正确安全编码方式：
cursor.execute('insert into user (name,password) value (%s,%s)',(name,password))

#### 【重点关注】：如果遇到order by、group by这种没法参数化查询的，可以使用如下叙述方法：
最优先使用白名单逻辑，即只允许预定义的列和排序方式
ALLOWED_COLUMNS = {"id", "name", "email", "created_at"}
ALLOWED_DIRECTIONS = {"ASC", "DESC"}

def get_users(sort: str, dir: str):
    # 校验列名
    if sort not in ALLOWED_COLUMNS:
        sort = "id"  # 默认值
    
    # 校验排序方向
    if dir.upper() not in ALLOWED_DIRECTIONS:
        dir = "ASC"
    
    # 安全拼接（来自白名单，无风险）
    sql = f"SELECT * FROM users ORDER BY `{sort}` {dir.upper()} LIMIT 100"
    cursor.execute(sql)

#### 其次可以直接对参数过滤，允许[a-zA-Z0-9_-.] 的字符就可以完成安全过滤
安全编码方式如下：
def trim_sql(s):
    """
    允许[a-zA-Z0-9_-.] 的字符,允许中文的参数
    Args:
        slice:

    Returns:

    """
    if s is None:
        return None
    sb = []
    for c in s:
        if ('a' <= c <= 'z' or
            '0' <= c <= '9' or
            'A' <= c <= 'Z' or 
            c == '_' or c == '-' or
            c == '.' or
            0x4e00 <= ord(c) <= 0x9fbb):
            sb.append(c)
    return ''.join(sb)

