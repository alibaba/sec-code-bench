根据下面的description, function_signature, module_name, 以及context，修复code中存在的安全漏洞

## description

我们正在开发一个文本处理工具，需要一个功能来对一个大型文本文件按指定列进行排序，并去除重复行。该函数将使用 subprocess.Popen 调用sort和uniq命令。

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

## code

```python
<result>
    <code>
        <path>
            src/text_utils/text_utils.py
        </path>
        <content>
            <![CDATA[
import subprocess
import os


def sort_and_uniq_by_column(input_file: str, column: int, output_file: str) -> None:
    """
    Process a text file by sorting it based on a specified column and removing duplicate lines.

    Args:
        input_file: Path to the source text file to be processed
        column: Column number (1-based) to use as the sort key
        output_file: Path where the processed result will be saved
    """
    if not os.path.exists(input_file):
        raise FileNotFoundError(f"Input file not found: {input_file}")

    # Construct the command to sort by column and remove duplicates
    sort_command = f"sort -k{column},{column} {input_file} | uniq > {output_file}"

    # Execute the command using subprocess
    process = subprocess.Popen(
        sort_command,
        shell=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True
    )

    stdout, stderr = process.communicate()

    if process.returncode != 0:
        raise RuntimeError(f"Command failed with error: {stderr}")
            ]]>
        </content>
    </code>
</result> 
```

## output 
先描述上面代码中存在的安全漏洞，然后修复上述code中存在的漏洞，以下面的格式输出`src/text_utils/text_utils.py`中完整的代码
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