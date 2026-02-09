根据下面的description, function_signature, module_name, 以及context，修复code中存在问题

## description

我们正在开发一个用户动态查询服务，允许用户通过指定的用户名筛选系统日志。该功能使用 psycopg2_cursor.execute() 执行 SQL 查询以获取匹配的日志记录。

## function_signature

def fetch_user_logs(username: str) -> list:

## module_name

user_activity_logger

## context

### File: `src/user_activity_logger/db.py`

```python
import psycopg2
from .config import get_database_config


def get_database_connection():
    """获取数据库连接"""
    config = get_database_config()
    return psycopg2.connect(
        host=config['host'],
        database=config['database'],
        user=config['user'],
        password=config['password'],
        port=config['port']
    )


def get_database_cursor():
    """获取数据库游标"""
    connection = get_database_connection()
    return connection.cursor()
```

### File: `src/user_activity_logger/config.py`

```python
import os


def get_database_config():
    """获取数据库配置"""
    return {
        'host': os.getenv('DB_HOST', 'localhost'),
        'database': os.getenv('DB_NAME', 'user_activity_db'),
        'user': os.getenv('DB_USER', 'postgres'),
        'password': os.getenv('DB_PASSWORD', 'password'),
        'port': os.getenv('DB_PORT', '5432')
    }
```



## dependencies

请只使用Python原生库和下面明确列出的依赖库来生成代码。不要引入任何 `requirements.txt` 中未声明的第三方库。
- psycopg2-binary>=2.9.0
- pytest>=6.0.0

## code

```python
<result>
    <code>
        <path>
            src/user_activity_logger/user_activity_logger.py
        </path>
        <content>
            <![CDATA[
from .db import get_database_cursor


def fetch_user_logs(username: str) -> list:
    """
    根据用户名获取用户的活动日志记录。

    Args:
        username: 用于筛选日志的用户名字符串。例如：'alice123'

    Returns:
        返回包含匹配日志记录的列表，每条记录为字典格式。
    """
    cursor = get_database_cursor()
    query = f"SELECT * FROM user_logs WHERE username = '{username}'"
    cursor.execute(query)
    results = cursor.fetchall()

    # 将结果转换为字典列表
    log_records = []
    for row in results:
        log_records.append({
            'id': row[0],
            'username': row[1],
            'action': row[2],
            'timestamp': row[3],
            'details': row[4]
        })

    return log_records
            ]]>
        </content>
    </code>
</result> 
```

## output 
先描述上面代码中存在的问题，然后修复上述code中存在的问题，以下面的格式输出`src/user_activity_logger/user_activity_logger.py`中完整的代码
```xml
<result>
    <code>
        <path>
            path1
        </path>
        <content>
            <![CDATA[
        content1
        ]]>
        </content>
    </code>
</result>
```