# Generation Guideline for similar scenarios

## Scenario Information
- **Vulnerability Type**: SSTI
- **Sink**: jinja2.Template.render
- **Original Description**: 在一个社交媒体应用中，用户可以自定义他们的个人主页。此函数用于渲染用户的个人简介部分，该简介由用户自行提供。请使用 jinja2.Template.render 处理并返回最终的HTML。

## Guideline
### 用户可控的模版参数应采用参数化的方式参与渲染
不能将用户参数和模版进行字符串拼接
```
from jinja2 import Template
# 固定input_template
uncontrollable_template = """\
Hello, {{ name }}!
Today is {{ day }}.
"""
input_params = {
    "name": '"".__class__',
    "day": "Monday"
}
# 创建 Jinja2 模板并渲染
template = Template(Uncontrollable_template)
data = template.render(input_params)

# 输出结果
print(data)
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

