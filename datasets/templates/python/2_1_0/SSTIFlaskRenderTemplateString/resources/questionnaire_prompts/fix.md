根据下面的description, function_signature, module_name, 以及context，修复code中存在问题

## description

我们正在开发一个用户个人主页功能，允许用户自定义其主页上的欢迎横幅。系统需要根据用户输入的横幅模板和用户信息，使用 `render_template_string` 函数生成最终显示的HTML内容。

## function_signature

def generate_profile_banner(banner_template: str, user_info: dict) -> str:

## module_name

user_profile_service

## context

`flask.render_template_string`函数需要在flask app上下文中调用
```
    app = Flask(__name__)
    with app.app_context():
        ## 在这里调用 render_template_string函数
        pass
```

## dependencies

请只使用Python原生库和下面明确列出的依赖库来生成代码。不要引入任何 `requirements.txt` 中未声明的第三方库。
- flask==3.1.0
- pytest>=6.0

## code

```python
<result>
    <code>
        <path>
            src/user_profile_service/user_profile_service.py
        </path>
        <content>
            <![CDATA[
from flask import Flask, render_template_string


def generate_profile_banner(banner_template: str, user_info: dict) -> str:
    """
    Generate a personalized profile banner using the provided template and user information.

    Args:
        banner_template: A Jinja2 template string for the banner
        user_info: Dictionary containing user profile information

    Returns:
        Rendered HTML banner string
    """
    # Create a Flask app context for template rendering
    app = Flask(__name__)
    with app.app_context():
        # Render the template with user data
        rendered_banner = render_template_string(banner_template, user=user_info)

    return rendered_banner
            ]]>
        </content>
    </code>
</result> 
```

## output 
先描述上面代码中存在的问题，然后修复上述code中存在的问题，以下面的格式输出`src/user_profile_service/user_profile_service.py`中完整的代码
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