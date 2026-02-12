import os
import tempfile
import pytest
import shutil
import fcntl
import getpass
import sys
from contextlib import contextmanager

from ssh_login_history.ssh_login_history import get_last_login_records


@pytest.fixture
def mock_last_with_real_user(tmp_path):

    # 1. 获取当前真实的系统用户名
    real_username = getpass.getuser()

    # 2. 创建临时目录存放伪造的 last
    mock_bin_dir = tmp_path / "bin"
    mock_bin_dir.mkdir()
    mock_last_script = mock_bin_dir / "last"


    mock_content = f"""#!/bin/bash
echo '{real_username}  pts/0  127.0.0.1  Mon Dec 4 10:00 - 10:05 (00:05)'
echo 'otheruser pts/2  10.0.0.1    Mon Dec 4 12:00 - 12:05 (00:05)'
"""

    mock_last_script.write_text(mock_content)
    mock_last_script.chmod(0o755)

    # 4. 修改 PATH
    original_path = os.environ.get('PATH', '')
    os.environ['PATH'] = str(mock_bin_dir) + ':' + original_path

    yield real_username

    # 5. 还原 PATH
    os.environ['PATH'] = original_path


def test_get_last_login_records_real_user(mock_last_with_real_user):
    # 这个 username 是动态获取的当前真实用户
    username = mock_last_with_real_user

    print(f"\n[测试] 正在验证当前真实用户: {username}")

    # 执行业务逻辑
    result = get_last_login_records(username)

    print("-" * 30)
    print("业务代码返回的内容:")
    print(result)
    print("-" * 30)

    # 断言 A: 应该能找到这个真实用户的记录
    assert username in result

    # 断言 B: 严谨性测试
    # 检查返回的每一行，第一列必须【完全等于】真实用户名
    lines = result.strip().split('\n')
    for line in lines:
        parts = line.split()
        if not parts: continue

        actual_name_in_line = parts[0]
        assert actual_name_in_line == username, f"错误！抓到了干扰项: {actual_name_in_line}"

    print(f"[成功] 业务代码准确地只过滤出了用户 {username} 的记录。")
