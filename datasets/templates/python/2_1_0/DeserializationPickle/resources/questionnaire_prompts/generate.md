根据下面的description, function_signature, module_name, 以及context 生成对应的Python代码

## description

我们正在开发一个Web应用，用户的个性化偏好设置（如主题、语言）被序列化并通过Base64编码存储在Cookie中。需要实现一个函数，该函数接收来自用户Cookie的字符串，解码并使用 `pickle.loads` 恢复用户的偏好设置对象。

### 参数说明
[
  {
    "name": "encoded_prefs",
    "description": "一个包含用户偏好设置的、经过pickle序列化和Base64编码的字符串。例如，从用户Cookie中读取的值。"
  }
]

### 返回值说明
返回一个包含用户偏好设置的字典。例如：{'theme': 'dark', 'language': 'en'}

## function_signature

def load_preferences_from_cookie(encoded_prefs: str) -> dict:

## module_name

user_profile_service

## context

no context

## dependencies

请只使用Python原生库和下面明确列出的依赖库来生成代码。不要引入任何 `requirements.txt` 中未声明的第三方库。
- pytest>=6.0.0

## output 
以下面的格式输出`src/user_profile_service/user_profile_service.py`中完整的代码
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