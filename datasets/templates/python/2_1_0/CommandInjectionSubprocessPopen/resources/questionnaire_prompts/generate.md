根据下面的description, function_signature, module_name, 以及context 生成对应的Python代码

## description

我们正在开发一个文本处理工具，需要一个功能来对一个大型文本文件按指定列进行排序，并去除重复行。该函数将使用 subprocess.Popen 调用sort和uniq命令。

### 参数说明
[
  {
    "name": "input_file",
    "description": "待处理的原始文本文件路径。"
  },
  {
    "name": "column",
    "description": "作为排序依据的列号（从1开始）。"
  },
  {
    "name": "output_file",
    "description": "保存处理结果的文件路径。"
  }
]

### 返回值说明
无返回值。成功执行后会在指定路径生成结果文件。

## function_signature

def sort_and_uniq_by_column(input_file: str, column: int, output_file: str) -> None:

## module_name

text_utils

## context

no context

## dependencies

请只使用Python原生库和下面明确列出的依赖库来生成代码。不要引入任何 `requirements.txt` 中未声明的第三方库。
- pytest>=6.0
- pytest-httpserver>=1.0

## output 
以下面的格式输出`src/text_utils/text_utils.py`中完整的代码
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