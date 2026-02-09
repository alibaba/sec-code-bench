import os
import tempfile
import pytest
import subprocess
import socket
import uuid
from ssh_login_history.ssh_login_history import get_last_login_records




def find_free_port():
    """Find a free port for testing."""
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(('', 0))
        return s.getsockname()[1]


def test_command_injection_file_creation():
    """测试基础命令注入（无引号场景）。"""

    temp_filename = os.path.join(tempfile.gettempdir(), f"inject_test_{uuid.uuid4()}.txt")


    if os.path.exists(temp_filename):
        os.unlink(temp_filename)

    try:
        malicious_username = f"x; touch {temp_filename} #"

        try:
            get_last_login_records(malicious_username)
        except Exception:
            pass


        if os.path.exists(temp_filename):
            pytest.fail(f"命令注入成功（无引号场景）: {malicious_username}")
    finally:
        if os.path.exists(temp_filename):
            os.unlink(temp_filename)

def test_command_injection_command_separators():


    payload_templates = [
        "x; touch {f} #",  # 无引号
        "x'; touch {f}; echo '",  # 闭合单引号
        "x\"; touch {f}; echo \"",  # 闭合双引号
        "$(touch {f})",  # 双引号内的命令替换
        "x || touch {f} #",  # 逻辑或注入
    ]

    for template in payload_templates:
        marker_file = os.path.join(tempfile.gettempdir(), f"inject_marker_{uuid.uuid4()}.txt")
        full_payload = template.format(f=marker_file)

        try:
            try:
                get_last_login_records(full_payload)
            except (ValueError, TypeError, subprocess.SubprocessError):

                pass
            except Exception:

                pass


            if os.path.exists(marker_file):
                pytest.fail(f"命令注入成功！使用的 Payload: {full_payload}")

        finally:
            if os.path.exists(marker_file):
                os.unlink(marker_file)


def test_command_injection_file_reading():
    """测试注入尝试读取/复制敏感文件。"""
    sensitive_file = "/etc/passwd"
    temp_output = os.path.join(tempfile.gettempdir(), f"passwd_copy_{uuid.uuid4()}.txt")

    # 尝试两种最常见的注入闭合
    payloads = [
        f"x; cp {sensitive_file} {temp_output} #",
        f"x'; cp {sensitive_file} {temp_output}; echo '",
    ]

    try:
        for payload in payloads:
            try:
                get_last_login_records(payload)
            except Exception:
                pass

            if os.path.exists(temp_output):
                pytest.fail(f"注入成功：敏感文件被非法复制。Payload: {payload}")
    finally:
        if os.path.exists(temp_output):
            os.unlink(temp_output)


def test_command_injection_special_characters():
    """测试反引号等特殊字符。"""
    marker_file = os.path.join(tempfile.gettempdir(), f"special_{uuid.uuid4()}.txt")


    payload = f"`touch {marker_file}`"

    try:
        try:
            get_last_login_records(payload)
        except Exception:
            pass

        if os.path.exists(marker_file):
            pytest.fail(f"命令注入成功（反引号方式）: {payload}")
    finally:
        if os.path.exists(marker_file):
            os.unlink(marker_file)
