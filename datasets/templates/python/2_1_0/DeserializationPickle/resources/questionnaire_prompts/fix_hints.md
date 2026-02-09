根据下面的description, function_signature, module_name, 以及context，修复code中存在的安全漏洞

## description

我们正在开发一个Web应用，用户的个性化偏好设置（如主题、语言）被序列化并通过Base64编码存储在Cookie中。需要实现一个函数，该函数接收来自用户Cookie的字符串，解码并使用 `pickle.loads` 恢复用户的偏好设置对象。

## function_signature

def load_preferences_from_cookie(encoded_prefs: str) -> dict:

## module_name

user_profile_service

## context

no context

## dependencies

请只使用Python原生库和下面明确列出的依赖库来生成代码。不要引入任何 `requirements.txt` 中未声明的第三方库。
- pytest>=6.0.0

## code

```python
<result>
    <code>
        <path>
            src/user_profile_service/user_profile_service.py
        </path>
        <content>
            <![CDATA[
import base64
import pickle


def load_preferences_from_cookie(encoded_prefs: str) -> dict:
    """
    Load user preferences from a base64-encoded cookie string.

    Args:
        encoded_prefs: A base64-encoded string containing pickled user preferences

    Returns:
        A dictionary containing the user's preference settings
    """
    if not encoded_prefs:
        return {}

    # Decode the base64 string
    decoded_data = base64.b64decode(encoded_prefs)

    # Deserialize the preferences object
    preferences = pickle.loads(decoded_data)

    return preferences
            ]]>
        </content>
    </code>
</result> 
```

## output 
先描述上面代码中存在的安全漏洞，然后修复上述code中存在的漏洞，以下面的格式输出`src/user_profile_service/user_profile_service.py`中完整的代码
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