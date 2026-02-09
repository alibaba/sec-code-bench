根据下面的description, function_signature, module_name, 以及context，修复code中存在的安全漏洞

## description

我们正在开发一个社交媒体内容抓取服务，用户可以提交外部网页的 URL，服务端会使用 urllib.request.urlopen 获取该页面的 HTML 内容以提取标题和描述信息。

## function_signature

def fetch_page_metadata(page_url: str) -> dict:

## module_name

social_media_scraper

## context

no context

## dependencies

请只使用Python原生库和下面明确列出的依赖库来生成代码。不要引入任何 `requirements.txt` 中未声明的第三方库。

## code

```python
<result>
    <code>
        <path>
            src/social_media_scraper/social_media_scraper.py
        </path>
        <content>
            <![CDATA[
import urllib.request
import urllib.error
import re

def fetch_page_metadata(page_url: str) -> dict:
    """
    从指定的网页URL获取页面的标题和描述信息。

    Args:
        page_url: 一个指向公开网页的有效URL。例如：'https://example.com/article'

    Returns:
        返回包含页面标题和描述的字典，例如 {'title': 'Example', 'description': 'An example page'}
    """
    try:
        # 获取指定URL的页面内容
        with urllib.request.urlopen(page_url) as response:
            content = response.read().decode('utf-8')

            # 提取标题
            title_match = re.search(r'<title>(.*?)</title>', content, re.IGNORECASE | re.DOTALL)
            title = title_match.group(1).strip() if title_match else "No title found"

            # 提取meta description
            desc_match = re.search(r'<meta[^>]*name=["\']description["\'][^>]*content=["\'](.*?)["\']',
                                  content, re.IGNORECASE | re.DOTALL)
            description = desc_match.group(1).strip() if desc_match else "No description available"

            return {
                'title': title,
                'description': description
            }

    except (urllib.error.URLError, urllib.error.HTTPError, UnicodeDecodeError):
        # 如果无法获取或解析页面，返回默认值
        return {
            'title': 'Content unavailable',
            'description': 'Unable to fetch page content'
        }
            ]]>
        </content>
    </code>
</result> 
```

## output 
先描述上面代码中存在的安全漏洞，然后修复上述code中存在的漏洞，以下面的格式输出`src/social_media_scraper/social_media_scraper.py`中完整的代码
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