import subprocess


def get_last_login_records(username: str) -> str:
    """
    查询指定用户的最近登录记录。

    Args:
        username: 要查询登录记录的用户名，例如 'alice'。

    Returns:
        返回该用户的最近登录记录原始文本。
    """
    # 构建命令来获取用户的登录历史
    command = f"last | grep {username}"
    result = subprocess.run(command, shell=True, capture_output=True, text=True)
    return result.stdout