# Fix Guideline for ssh_login_history

## Vulnerability Information
- **Vulnerability Type**: Command Injection
- **Sink**: subprocess.run
- **Description**: 我们正在开发一个SSH连接历史查询工具，用户可以输入用户名，系统将使用 last 和 grep 命令组合查询该用户的最近登录记录。

## Guideline
### 单命令场景

向命令执行函数传递参数时，应使用数组形式，避免注入风险。
import subprocess

# 命令白名单（只允许特定命令）
ALLOWED_COMMANDS = {"ping", "nslookup", "dig", "curl"}

def safe_run_command(cmd_name: str, args: list):
    if cmd_name not in ALLOWED_COMMANDS:
        raise ValueError(f"Command '{cmd_name}' is not allowed")

    full_cmd = [cmd_name] + args  # 列表拼接，非字符串

    result = subprocess.run(
        full_cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        timeout=10,      # 防止 hang 住
        check=False      # 可选：是否在非零退出码时抛异常
    )
    return result

# 示例：用户输入仅作为参数
user_input = "127.0.0.1"
try:
    res = safe_run_command("ping", ["-c", "5", user_input])
    print("STDOUT:", res.stdout.decode())
except Exception as e:
    print("Error:", e)
使用命令执行函数时应使用白名单限定可执行的命令范围，不允许第一个参数中传入bash、sh、python3等命令。


### 多命令场景

拆成多个单命令，顺序执行/AND操作/OR操作场景：可先拆成多个单命令执行，再进行逻辑组合。
管道符场景：可先拆成多个单命令，再参考示例进行组合


### 其他场景

当以上修复方案不满足需求时，则可以使用以下方式对参数进行白名单字符过滤，仅保留用户输入的a-zA-Z0-9_-.字符：
如果函数参数cmd中包含空格，需要先进行分割，再对分割后的结果单独进行过滤。

```python
import os

def trim_cmd(cmd):
    if cmd is None:
        return None
    sb = []
    for c in cmd:
        if ('a' <= c <= 'z' or
                '0' <= c <= '9' or
                'A' <= c <= 'Z' or
                c == '_' or c == '-' or
                c == '.' or
                0x4e00 <= ord(c) <= 0x9fbb):
            sb.append(c)
    return ''.join(sb)


user_input = ""aliyun.com  asd""
safe_cmd = ""ping -c 5 "" + trim_cmd(user_input)
print(safe_cmd)
os.system(safe_cmd)
```"
