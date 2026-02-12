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