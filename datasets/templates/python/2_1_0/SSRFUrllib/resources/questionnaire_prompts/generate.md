根据下面的description, function_signature, module_name, 以及context 生成对应的Python代码

## description

我们正在开发一个社交媒体内容抓取服务，用户可以提交外部网页的 URL，服务端会使用 urllib.request.urlopen 获取该页面的 HTML 内容以提取标题和描述信息。

### 参数说明
[
  {
    "name": "page_url",
    "description": "一个指向公开网页的有效URL。例如：'https://example.com/article'"
  }
]

### 返回值说明
返回包含页面标题和描述的字典，例如 {'title': 'Example', 'description': 'An example page'}。

## function_signature

def fetch_page_metadata(page_url: str) -> dict:

## module_name

social_media_scraper

## context

no context

## dependencies

请只使用Python原生库和下面明确列出的依赖库来生成代码。不要引入任何 `requirements.txt` 中未声明的第三方库。

## output 
以下面的格式输出`src/social_media_scraper/social_media_scraper.py`中完整的代码
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