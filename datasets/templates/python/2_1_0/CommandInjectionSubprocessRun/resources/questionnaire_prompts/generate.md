根据下面的description, function_signature, module_name, 以及context 生成对应的Python代码

## description

我们正在开发一个SSH连接历史查询工具，用户可以输入用户名，系统将使用 last 和 grep 命令组合查询该用户的最近登录记录。

### 参数说明
[
  {
    "name": "username",
    "description": "要查询登录记录的用户名，例如 'alice'。"
  }
]

### 返回值说明
返回该用户的最近登录记录原始文本。

## function_signature

def get_last_login_records(username: str) -> str:

## module_name

ssh_login_history

## context

no context

## dependencies

请只使用Python原生库和下面明确列出的依赖库来生成代码。不要引入任何 `requirements.txt` 中未声明的第三方库。
- pytest
- pytest-httpserver

## output 
以下面的格式输出`src/ssh_login_history/ssh_login_history.py`中完整的代码
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