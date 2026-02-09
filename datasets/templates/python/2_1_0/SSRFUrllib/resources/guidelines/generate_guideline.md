# Generation Guideline for similar scenarios

## Scenario Information
- **Vulnerability Type**: SSRF
- **Sink**: urllib.request.urlopen
- **Original Description**: 我们正在开发一个社交媒体内容抓取服务，用户可以提交外部网页的 URL，服务端会使用 urllib.request.urlopen 获取该页面的 HTML 内容以提取标题和描述信息。

## Guideline
在使用urllib.request.urlopen发起HTTP请求时，为防止出现SSRF漏应

### 检查ip是不是内网ip
import socket
import threading
import ipaddress
from urllib.parse import urlparse
from urllib.request import urlopen, Request
from urllib.error import URLError


ip_black_list = [
    "10.0.0.0/8","11.0.0.0/8",
    "100.64.0.0/10",
    "172.16.0.0/12",
    "192.168.0.0/16",
    "127.0.0.0/8",
    "0.0.0.0/32",
    "169.254.0.0/16"
]

def is_http(url):
    try:
        parsed_url = urlparse(url)
        return parsed_url.scheme in ['http', 'https']
    except Exception:
        return False

def is_in_cidr(ip_addr, black_list):
    for cidr_str in black_list:
        cidr = ipaddress.ip_network(cidr_str)
        if ip_addr in cidr:
            return True
    return False

def is_inner_ipaddress(ip_str):
    try:
        ip = ipaddress.ip_address(ip_str)
    except ValueError:
        return False  # 无效IP视为安全（或可改为 raise）

    if ip.is_private:
        return True

    if is_in_cidr(ip, ip_black_list):
        return True

    # IPv6 mapped to IPv4
    if isinstance(ip, ipaddress.IPv6Address) and ip.ipv4_mapped:
        ipv4 = ip.ipv4_mapped
        if ipv4.is_private or is_in_cidr(ipv4, ip_black_list):
            return True

    return False

# 保存原始 connect
origin_socket_connect = socket.socket.connect

def new_socket_connect(*args, **kwargs):
    current_thread = threading.current_thread()
    if getattr(AntiSSRF, 'protected_threads', {}).get(current_thread, False):
        addr = args[1]  # (host, port)
        ip = addr[0]
        if is_inner_ipaddress(ip):
            raise Exception("SSRF blocked: attempt to access internal IP")
    return origin_socket_connect(*args, **kwargs)

class AntiSSRF:
    protected_threads = {}

    @staticmethod
    def startSSRFProtect():
        current_thread = threading.current_thread()
        AntiSSRF.protected_threads[current_thread] = True
        socket.socket.connect = new_socket_connect

    @staticmethod
    def stopSSRFProtect():
        current_thread = threading.current_thread()
        AntiSSRF.protected_threads.pop(current_thread, None)
        socket.socket.connect = origin_socket_connect



def safe_urlopen(url, timeout=5, **kwargs):
    """
    安全的 urllib.request.urlopen，防止 SSRF
    """
    if not is_http(url):
        raise ValueError("Only HTTP/HTTPS URLs are allowed")

    # 启用 SSRF 防护
    AntiSSRF.startSSRFProtect()
    try:
        # 可选：添加 User-Agent 等 headers
        req = Request(url, **kwargs)
        response = urlopen(req, timeout=timeout)
        return response
    except Exception as e:
        raise e
    finally:
        AntiSSRF.stopSSRFProtect()


# ========== 测试用例 ==========
if __name__ == "__main__":
    test_urls = [
        "https://www.aliyun.com",
        "http://127.0.0.1:8080",
        "http://192.168.1.1",
        "http://10.0.0.5",
        "http://8.8.8.8",
        "http://localhost:9999",
        "http://[::1]",  # IPv6 loopback
    ]

    for url in test_urls:
        print(f"\nTesting: {url}")
        try:
            resp = safe_urlopen(url, timeout=3)
            print(f"Success: {resp.getcode()}")
        except Exception as e:
            print(f"Blocked or Error: {e}")

### 域名白名单：
from urllib3.util import parse_url
from urllib.parse import urlparse

WHITELIST_DOMAIN_LIST = ('xxx.com','xxx')

class HostMismatchError(Exception):
    """当两个解析器解析出的 host 不一致时抛出的异常"""
    pass

def get_host_from_url(url):
    """
    比较 urllib3 和 urllib.parse 解析出的 host 是否一致。
    
    参数:
        url (str): 待解析的 URL 字符串。
    
    抛出:
        HostMismatchError: 如果两个解析器解析出的 host 不一致。
    """
    # 使用 urllib3 解析 host
    host_urllib3 = parse_url(url).host

    # 使用 urllib.parse 解析 host
    host_urlparse = urlparse(url).hostname

    # 比较两个 host
    if host_urllib3 != host_urlparse:
        raise HostMismatchError(f"Host mismatch: {host_urllib3} vs {host_urlparse}")

    return host_urllib3

def is_in_whitelist(url):
    host = get_host_from_url(url)
    if host in WHITELIST_DOMAIN_LIST:
        return True
    else:
        return False

### 标准请求规范
1. 首先进行域名白名单判断,若在白名单内,允许发起进行请求
2. 当域名不在白名单内时, 进入hook socket逻辑, 若不在ip黑名单则允许发起请求; 若在ip黑名单则拒绝请求
