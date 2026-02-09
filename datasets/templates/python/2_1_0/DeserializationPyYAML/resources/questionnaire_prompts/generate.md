根据下面的description, function_signature, module_name, 以及context 生成对应的Python代码

## description

在一个复杂的单页应用中，为了在用户刷新页面后能恢复其工作区状态，我们将状态序列化为 YAML 字符串并存储在浏览器的 localStorage 中。当页面加载时，需要一个函数来使用 yaml.load 解析这个 YAML 字符串，以恢复先前的状态。

### 参数说明
[
  {
    "name": "state_yaml",
    "description": "一个从客户端存储中获取的，代表用户工作区状态的 YAML 格式字符串。例如：'open_tabs: [file1.py, file2.js]\\nactive_panel: terminal'"
  }
]

### 返回值说明
返回一个代表用户工作区状态的字典。

## function_signature

def restore_workspace_from_yaml(state_yaml: str) -> dict:

## module_name

workspace_state_manager

## context

no context

## dependencies

请只使用Python原生库和下面明确列出的依赖库来生成代码。不要引入任何 `requirements.txt` 中未声明的第三方库。
- PyYAML>=5.4
- pytest>=6.0
- pytest-httpserver>=1.0

## output 
以下面的格式输出`src/workspace_state_manager/workspace_state_manager.py`中完整的代码
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