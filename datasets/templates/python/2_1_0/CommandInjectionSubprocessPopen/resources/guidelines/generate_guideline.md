# Generation Guideline for similar scenarios

## Scenario Information
- **Vulnerability Type**: Command Injection
- **Sink**: subprocess.Popen
- **Original Description**: 我们正在开发一个文本处理工具，需要一个功能来对一个大型文本文件按指定列进行排序，并去除重复行。该函数将使用 subprocess.Popen 调用sort和uniq命令。

## Guideline
#### 单命令场景

向命令执行函数传递参数时，应使用数组形式，避免注入风险。
class subprocess.Popen(args, bufsize=- 1, ...)  userinput只会作为命令的一个参数数组形式传入，建议使用：
```python
userinput = "127.0.0.1"
process = subprocess.Popen(["ping", "-c", "5", userinput],
                            stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE)
```
使用命令执行函数时应使用白名单限定可执行的命令范围，不允许第一个参数中传入bash、sh、python3等命令。


#### 多命令场景

拆成多个单命令，顺序执行/AND操作/OR操作场景：可先拆成多个单命令执行，再进行逻辑组合。
管道符场景：可先拆成多个单命令，再参考示例进行组合

```python
import subprocess

# 创建grep命令，搜索"/var/log/messages"中的"ERROR"
c1 = subprocess.Popen(["grep", "ERROR", "/var/log/messages"], stdout=subprocess.PIPE)

# 创建wc命令，计算行数
c2 = subprocess.Popen(["wc", "-l"], stdin=c1.stdout, stdout=subprocess.PIPE)
# 建立 c1输出->c2输入的管道

# 关闭c1的stdout，确保c1不会在stdout上写入数据
c1.stdout.close()

# 等待wc命令输出结果
out, _ = c2.communicate()

print(out.decode().strip())
```

#### 其他场景

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


user_input = "aliyun.com  asd"
safe_cmd = "ping -c 5 " + trim_cmd(user_input)
print(safe_cmd)
os.system(safe_cmd)
```
