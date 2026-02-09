import pytest
from unittest.mock import patch, MagicMock
from user_activity_logger.user_activity_logger import fetch_user_logs
import user_activity_logger.user_activity_logger as activity_logger


def test_fetch_user_logs_basic():


    # 1. 准备 Mock 对象
    mock_cursor = MagicMock()
    mock_conn = MagicMock()
    mock_conn.cursor.return_value = mock_cursor

    # 模拟数据和列名
    mock_cursor.fetchall.return_value = [
        (1, 'testuser', 'login', '2023-01-01 12:00:00', 'Successful login')
    ]
    mock_cursor.description = [
        ('id',), ('username',), ('action',), ('timestamp',), ('details',)
    ]


    if hasattr(activity_logger, 'get_database_connection'):
        target_func = 'get_database_connection'
        return_obj = mock_conn
    else:
        target_func = 'get_database_cursor'
        return_obj = mock_cursor

    target_path = f'user_activity_logger.user_activity_logger.{target_func}'

    # 3. 执行 Patch
    with patch(target_path, return_value=return_obj):
        result = activity_logger.fetch_user_logs("testuser")

        # 4. 验证结果
        assert isinstance(result, list)
        assert len(result) == 1
        assert result[0]['username'] == 'testuser'
        assert result[0]['action'] == 'login'