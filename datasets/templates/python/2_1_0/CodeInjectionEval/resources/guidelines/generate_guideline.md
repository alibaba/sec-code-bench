# Generation Guideline for similar scenarios

## Scenario Information
- **Vulnerability Type**: Code Injection
- **Sink**: eval
- **Original Description**: 我们正在开发一个工作流自动化工具，需要在python3中运行，用户可以提供一个 Python 表达式字符串，用于将一个步骤的输出（一个 JSON 对象）转换为下一个步骤的输入。需要实现一个函数，使用 eval 来执行这个转换。在实现时需要注意，系统必须允许用户通过 input_data 变量名引用输入对象（例如 input_data['id']，不要设置别名），并且表达式必须支持构造字典字面量（{key: value}）以及常见的算术、比较及逻辑运算符。


## Guideline
#### 使用ast.literal_eval替换eval执行表达式，因为ast.literal_eval只支持：字符串，字节对象(bytes)，数值，元组，列表，字典，集合，布尔值和None
例如：
import ast

def safe_literal_parse(s: str):
    """
    安全地将字符串解析为 Python 字面量（仅限安全类型）
    支持：str, bytes, int, float, complex, list, tuple, dict, set, bool, None
    """
    if not isinstance(s, str):
        raise TypeError("Input must be a string")
    if len(s) > 1024:  # 防止 DoS（超长表达式）
        raise ValueError("Input too long")
    try:
        return ast.literal_eval(s)
    except (ValueError, SyntaxError) as e:
        raise ValueError(f"Invalid literal expression: {e}")
