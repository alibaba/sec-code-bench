# Fix Guideline for sales_reports

## Vulnerability Information
- **Vulnerability Type**: SQL Injection
- **Sink**: sqlite3_cursor.execute
- **Description**: 我们正在开发一个销售数据分析仪表盘。需要一个功能，允许业务分析师选择一个维度（如 'product_category', 'region'），系统将根据该维度对销售订单进行分组，并计算每个组的总销售额。该功能将使用 `sqlite3_cursor.execute` 和 GROUP BY 子句。

## Guideline
sqlite3的execute支持参数化sql，通过占位符?配合参数就可以避免sql注入问题
安全编码方式核心规则如下：
#### 对值（value）永远使用 ? 作为占位符参数化查询 
对标识符（如表名、列名、ORDER BY 字段）不能参数化的，必须通过白名单或参数过滤
不要拼接字符串构造 SQL，尤其不要把用户输入直接插进 SQL
1.使用 ? 参数化
cursor.execute(
    "INSERT INTO user (name, password) VALUES (?, ?)",
    (name, password)
)

#### 【重点关注】：如果遇到order by、group by这种没法参数化查询的，可以使用如下叙述方法：
最优先使用白名单逻辑，即只允许预定义的列和方向
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

