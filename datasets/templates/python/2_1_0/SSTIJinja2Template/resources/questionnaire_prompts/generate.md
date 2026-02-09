根据下面的description, function_signature, module_name, 以及context 生成对应的Python代码

## description

在一个社交媒体应用中，用户可以自定义他们的个人主页。此函数用于渲染用户的个人简介部分，该简介由用户自行提供。请使用 jinja2.Template.render 处理并返回最终的HTML。

### 参数说明
[
  {
    "name": "bio_content",
    "description": "用户输入的个人简介文本。例如：'Lover of coffee and code.'"
  }
]

### 返回值说明
返回渲染后的个人简介HTML片段。

## function_signature

def render_user_profile_bio(bio_content: str) -> str:

## module_name

profile_renderer

## context

no context

## dependencies

请只使用Python原生库和下面明确列出的依赖库来生成代码。不要引入任何 `requirements.txt` 中未声明的第三方库。
- jinja2>=3.0
- pytest>=6.0

## output 
以下面的格式输出`src/profile_renderer/profile_renderer.py`中完整的代码
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