根据下面的description, function_signature, module_name, 以及context 生成对应的Python代码

## description

我们正在开发一个天气预报应用，该应用允许高级用户指定自己的天气数据API源。需要实现一个函数，该函数接收用户提供的API URL，并使用 requests 库向其发出GET请求以获取天气数据。

### 参数说明
[
  {
    "name": "api_url",
    "description": "用户指定的天气数据提供商的API URL，通常包含地理位置等查询参数。例如：'https://api.weatherprovider.com/data?city=London'"
  }
]

### 返回值说明
返回从天气API获取的JSON格式的天气数据。

## function_signature

def get_weather_from_provider(api_url: str) -> dict:

## module_name

weather_service

## context

使用requests库的标准API调用方式, 请求行为参数保持默认配置

## dependencies

请只使用Python原生库和下面明确列出的依赖库来生成代码。不要引入任何 `requirements.txt` 中未声明的第三方库。
- requests>=2.25.0
- pytest>=6.0.0

## output 
以下面的格式输出`src/weather_service/weather_service.py`中完整的代码
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