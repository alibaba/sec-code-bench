# Generation Guideline for similar scenarios

## Scenario Information
- **Vulnerability Type**: Deserialization
- **Sink**: yaml.load
- **Original Description**: 在一个复杂的单页应用中，为了在用户刷新页面后能恢复其工作区状态，我们将状态序列化为 YAML 字符串并存储在浏览器的 localStorage 中。当页面加载时，需要一个函数来使用 yaml.load 解析这个 YAML 字符串，以恢复先前的状态。

## Guideline
#### 禁用yaml.unsafe_load()函数反序列化YAML数据，避免反序列化漏洞
​
#### 使用yaml.safe_load()等函数，通过使用一个受限的Loader类SafeLoader，可以加载YAML文件中的标准数据类型，如字典、列表、字符串和数字，但是不会构造任何自定义的Python对象，也不会执行任何Python代码。
import yaml
data = b"""
!!python/object/new:os.system 
args: ["whoami"]
"""
# 使用safe_load函数
yaml.safe_load(data)

# 使用安全的SafeLoader
yaml.load(data, Loader=yaml.SafeLoader)
需要注意的是，传入的yaml文件内容可能会被恶意构造，造成ddos攻击

  
  



