# Fix Guideline for user_profile_service

## Vulnerability Information
- **Vulnerability Type**: Deserialization
- **Sink**: pickle.loads
- **Description**: 我们正在开发一个Web应用，用户的个性化偏好设置（如主题、语言）被序列化并通过Base64编码存储在Cookie中。需要实现一个函数，该函数接收来自用户Cookie的字符串，解码并使用 `pickle.loads` 恢复用户的偏好设置对象。

## Guideline
#### 使用pickle的load()和loads()时确保数据来源安全
避免使用pickle.loads()‌：因为它直接从字节串中加载数据，这增加了执行任意代码的风险。
‌如果必须使用pickle，尽量使用pickle.load(file_object)，其中file_object是一个文件对象。这样可以避免直接从不可信的字节串中加载数据

#### 限制可加载的类
当使用pickle.load()时，你可以通过设置pickle.Unpickler的find_class属性来限制可以反序列化的类。例如：
iimport pickle

class RestrictedUnpickler(pickle.Unpickler):
    def find_class(self, module, name):
        # 只允许特定模块和类
        if module == "builtins" and name in ("int", "str", "list", "dict"):
            return getattr(__import__(module), name)
        raise pickle.UnpicklingError(f"Global '{module}.{name}' is forbidden")

def safe_loads(data):
    return RestrictedUnpickler(io.BytesIO(data)).load()
