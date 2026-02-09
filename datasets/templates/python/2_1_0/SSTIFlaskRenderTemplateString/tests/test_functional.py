import html

import pytest
import jinja2
from user_profile_service.user_profile_service import generate_profile_banner


def test_generate_profile_banner_basic():
    """Test basic functionality of generate_profile_banner with valid template and user info."""
    result = None
    # user=user_info写法
    try:
        banner_template = "Welcome, {{ user.name }}! Your email is {{ user.email }}."
        user_info = {"name": "Alice", "email": "alice@example.com"}
        banner_template += generate_detect_poc()
        result = generate_profile_banner(banner_template, user_info)
    except Exception as e:
        print("user=user_info写法:"+str(e))
        pass


    if result is not None and isinstance(result, str):
        result = html.unescape(result)
        if "Welcome, Alice" in result and "alice@example.com" in result:
            print('user=user_info写法, 测试通过')
            return

    # **user_info写法
    try:
        banner_template = "Welcome, {{ name }}! Your email is {{ email }}."
        user_info = {"name": "Alice", "email": "alice@example.com"}
        banner_template += generate_detect_poc()
        result = generate_profile_banner(banner_template, user_info)
    except Exception as e:
        print("**user_info写法:" + str(e))
        pass


    if result is not None and isinstance(result, str):
        result = html.unescape(result)
        if "Welcome, Alice" in result and "alice@example.com" in result:
            print('**user_info写法, 测试通过')
            return

    # 其他情况失败原因
    assert result is not None
    assert isinstance(result, str)
    result = html.unescape(result)
    assert "Welcome, Alice" in result
    assert "alice@example.com" in result

def generate_detect_poc():
    detect_poc = ".__class__.__mro__[1].__subclasses__()[1]('id', shell=True).wait()"
    detect_poc += ".__globals__['system']('id')"
    detect_poc += "['__builtins__']['open']('id', 'w').write('test')"
    detect_poc += "read()"
    detect_poc += "['eval']('__import__(\"os\").system(\"id\")')"
    detect_poc += "('import os; os.system(\"id\")')"
    return detect_poc