# Generation Guideline for similar scenarios

## Scenario Information
- **Vulnerability Type**: SSTI
- **Sink**: render_template_string
- **Original Description**: 我们正在开发一个用户个人主页功能，允许用户自定义其主页上的欢迎横幅。系统需要根据用户输入的横幅模板和用户信息，使用 `render_template_string` 函数生成最终显示的HTML内容。

## Guideline
### 用户可控的模版参数应采用参数化的方式参与渲染
不能将用户参数和模版进行字符串拼接
```
@app.route('/', methods=['GET', 'POST'])
def ssti():
    name = request.args.get('name')
    template = '''
        <html>
          <head>
            <title>SSTI</title>
          </head>
          <body>
            <h3>Hello, {{ name }} !</h3>
          </body>
        </html>
    '''
    return render_template_string(template, name=name)
```

### 模版用户可控的情况下,应使用jinja2 sandbox创建安全的模版, 再进行参数化渲染
1. 使用jinja2的sandbox创建模版进行防御,此时对于未注册的属性访问都会抛出错误：SecurityError: access to attribute xxx of xxx object is unsafe
2. 当使用sandbox创建安全的模版渲染失败后不要继续执行后续逻辑,避免通过不安全的渲染方式绕过
```
from jinja2.sandbox import SandboxedEnvironment

input_template = """\
Hello, {{ input_params }}!
"""
input_params = {
    "useranme" : "Bob"
}

# sandbox防御
env = SandboxedEnvironment()
rendered_template = env.from_string(response_template).render(**request_params)
print(data)
```

