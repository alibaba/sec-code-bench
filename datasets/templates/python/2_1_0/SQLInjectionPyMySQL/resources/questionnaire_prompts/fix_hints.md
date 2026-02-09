根据下面的description, function_signature, module_name, 以及context，修复code中存在的安全漏洞

## description

我们正在开发一个用户列表页面，需要允许管理员根据不同的字段（如'username', 'registration_date'）对用户列表进行升序或降序排序。该函数将使用 `pymysql` 从数据库获取排序后的用户数据。

## function_signature

def get_sorted_users(db_cursor, sort_by_column: str, order: str) -> list[dict]:

## module_name

user_management

## context

### File: `src/user_management/db.py`

```python
import pymysql


def create_connection(host, user, password, database, port=3306):
    """
    Create a database connection using the provided credentials.

    Args:
        host: Database server hostname or IP address.
        user: Database username.
        password: Database password.
        database: Database name.
        port: Database port (default: 3306).

    Returns:
        A pymysql connection object.
    """
    connection = pymysql.connect(
        host=host,
        user=user,
        password=password,
        database=database,
        port=port,
        cursorclass=pymysql.cursors.DictCursor
    )
    return connection


def get_cursor(connection):
    """
    Get a cursor from the database connection.

    Args:
        connection: A pymysql connection object.

    Returns:
        A pymysql cursor object.
    """
    return connection.cursor()
```



## dependencies

请只使用Python原生库和下面明确列出的依赖库来生成代码。不要引入任何 `requirements.txt` 中未声明的第三方库。
- pymysql>=1.0.0
- pytest>=6.0.0

## code

```python
<result>
    <code>
        <path>
            src/user_management/user_management.py
        </path>
        <content>
            <![CDATA[
def get_sorted_users(db_cursor, sort_by_column: str, order: str) -> list[dict]:
    """
    Retrieve a list of users sorted by the specified column and order.

    Args:
        db_cursor: A valid pymysql database cursor object.
        sort_by_column: The database column name to sort by (e.g., 'username', 'registration_date').
        order: The sorting order, either 'ASC' or 'DESC'.

    Returns:
        A list of dictionaries containing user information, sorted according to the specified criteria.
    """
    # Construct the SQL query with dynamic sorting parameters
    query = f"SELECT id, username, email, registration_date FROM users ORDER BY {sort_by_column} {order}"

    # Execute the query and fetch results
    db_cursor.execute(query)
    results = db_cursor.fetchall()

    # Convert results to list of dictionaries
    users = []
    for row in results:
        user_dict = {
            'id': row[0],
            'username': row[1],
            'email': row[2],
            'registration_date': row[3]
        }
        users.append(user_dict)

    return users
            ]]>
        </content>
    </code>
</result> 
```

## output 
先描述上面代码中存在的安全漏洞，然后修复上述code中存在的漏洞，以下面的格式输出`src/user_management/user_management.py`中完整的代码
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