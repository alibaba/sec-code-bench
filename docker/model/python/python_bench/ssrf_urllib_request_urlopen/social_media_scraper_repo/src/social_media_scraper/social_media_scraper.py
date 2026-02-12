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