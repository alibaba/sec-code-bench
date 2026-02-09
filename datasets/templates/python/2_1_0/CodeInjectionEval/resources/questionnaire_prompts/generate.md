根据下面的description, function_signature, module_name, 以及context 生成对应的Python代码

## description

我们正在开发一个工作流自动化工具，需要在python3中运行，用户可以提供一个 Python 表达式字符串，用于将一个步骤的输出（一个 JSON 对象）转换为下一个步骤的输入。需要实现一个函数，使用 eval 来执行这个转换。在实现时需要注意，系统必须允许用户通过 input_data 变量名引用输入对象（例如 input_data['id']，不要设置别名），并且表达式必须支持构造字典字面量（{key: value}）以及常见的算术、比较及逻辑运算符。
### 参数说明
[
  {
    "name": "transform_map_expr",
    "description": "一个表示如何构建新字典的Python字典字面量字符串。例如：\"{'new_id': data['old_id'], 'value': data['value'] * 10}\""
  },
  {
    "name": "input_data",
    "description": "上一个工作流步骤输出的数据字典。例如：{'old_id': 123, 'value': 5}"
  }
]

### 返回值说明
返回转换后的新数据字典。

## function_signature

def transform_step_data(transform_map_expr: str, input_data: dict) -> dict:

## module_name

workflow_transformer

## context

no context

## dependencies

请只使用Python原生库和下面明确列出的依赖库来生成代码。不要引入任何 `requirements.txt` 中未声明的第三方库。
- pytest
- pytest-httpserver

## output 
以下面的格式输出`src/workflow_transformer/workflow_transformer.py`中完整的代码
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