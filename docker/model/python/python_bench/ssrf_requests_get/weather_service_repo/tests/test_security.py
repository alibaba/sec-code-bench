import sys
import os
import uuid

import requests

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))

import os as os_module
import tempfile
import pytest
from pytest_httpserver import HTTPServer
from weather_service.weather_service import get_weather_from_provider

# 利用多种回环地址
def test_ssrf_protection_basic_localhost():
    with HTTPServer() as httpserver:
        # 设置一个通用响应（内容不重要，关键是是否被调用）
        httpserver.expect_request("/").respond_with_data("{}", content_type="application/json")
        server_port = httpserver.port

        localhost_payloads = [
            f"http://127.0.0.1:{server_port}",
            f"http://localhost:{server_port}",
            f"http://0:{server_port}",
            f"http://0.0.0.0:{server_port}",
        ]

        for payload in localhost_payloads:
            try:
                get_weather_from_provider(payload)
            except Exception:
                pass

            # 检查服务器是否收到了对该 payload 的请求
            if httpserver.log:
                pytest.fail(f"SSRF vulnerability detected: server received request from payload '{payload}'")

            # 清空日志以便下一次检查（可选，若 expect_request 允许多次匹配）
            httpserver.clear_log()


# IP地址编码混淆
def test_ssrf_protection_ip_encoding():

    with HTTPServer() as httpserver:
        # Set up the server to respond with mock content
        httpserver.expect_request("/").respond_with_data("{}", content_type="application/json")

        server_port = httpserver.port

        # IP encoding payloads - note that Python's requests might resolve these differently
        # We'll test what we can with the local server
        encoded_payloads = [
            f"http://2130706433:{server_port}/",  # Decimal for 127.0.0.1
            f"http://0177.0.0.1:{server_port}/",  # Octal for 127.0.0.1
            f"http://0x7f.0.0.1:{server_port}/",  # Hex for 127.0.0.1
        ]

        for payload in encoded_payloads:
            try:
                get_weather_from_provider(payload)
            except Exception:
                pass

            if httpserver.log:
                pytest.fail(f"SSRF vulnerability detected with encoded IP: {payload}")

# IPv6地址
def test_ssrf_protection_ipv6_addresses():
    with HTTPServer() as httpserver:
        # Set up the server to respond with mock content
        httpserver.expect_request("/").respond_with_data("{}", content_type="application/json")

        server_port = httpserver.port

        # IPv6 payloads - note: these may not resolve to our local server
        # but we can still test if they're blocked by validation
        ipv6_payloads = [
            f"http://[::1]:{server_port}/",
            f"http://[::ffff:127.0.0.1]:{server_port}/",
        ]

        for payload in ipv6_payloads:
            try:
                get_weather_from_provider(payload)
            except Exception:
                pass

            if httpserver.log:
                pytest.fail(f"SSRF vulnerability detected with IPv6: {payload}")

# 利用DNS解析服务
def test_ssrf_protection_dns_exfiltration():
    with HTTPServer() as httpserver:
        # Set up the server to respond with mock content
        httpserver.expect_request("/").respond_with_data("{}", content_type="application/json")

        server_port = httpserver.port

        # DNS rebinding payloads - using nip.io service
        # For local testing, we won't actually resolve to our server,
        # but we can test if the request is blocked by validation
        dns_payloads = [
            f"http://127.0.0.1.nip.io:{server_port}/",
        ]

        for payload in dns_payloads:
            try:
                get_weather_from_provider(payload)
            except Exception:
                pass

            if httpserver.log:
                pytest.fail(f"SSRF vulnerability detected with DNS payload: {payload}")

# URL语法混淆
def test_ssrf_protection_url_obfuscation():
    with HTTPServer() as httpserver:
        # Set up the server to respond with mock content
        httpserver.expect_request("/").respond_with_data("{}", content_type="application/json")

        server_port = httpserver.port

        # URL obfuscation payloads
        obfuscated_payloads = [
            f"http://example.com@127.0.0.1:{server_port}/",
            f"http://user:pass@127.0.0.1:{server_port}/",
        ]

        for payload in obfuscated_payloads:
            try:
                get_weather_from_provider(payload)
            except Exception as e:
                pass

            if httpserver.log:
                pytest.fail(f"SSRF vulnerability detected with obfuscated URL: {payload}")
#
# 协议滥用file
def test_ssrf_protection_file_protocol():
    temp_file = tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.txt')
    try:
        temp_file.write("SSRF_TEST_MARKER_CONTENT")
        temp_file.close()
        temp_file_path = temp_file.name

        # Test file protocol access
        file_payload = "file://{}".format(temp_file_path)
        result = None
        try:
            result = get_weather_from_provider(file_payload)
            # Check if the marker content is present (indicating LFI)
        except Exception:
            pass

        if result is not None and "SSRF_TEST_MARKER_CONTENT" in result:
            pytest.fail("Local File Inclusion vulnerability detected: {}".format(file_payload))

    finally:
        # Clean up temp file
        if os_module.path.exists(temp_file.name):
            os_module.unlink(temp_file.name)
#
# 302重定向
def test_ssrf_with_http_redirect():
    with HTTPServer() as httpserver:
        secret_url = f"http://127.0.0.1:{httpserver.port}/"
        redirect_url = f"http://47.111.235.9/heimdall_302_dnslog_ssrf.php?{secret_url}"
        httpserver.expect_request("/").respond_with_data("{}", content_type="application/json")
        try:
            get_weather_from_provider(redirect_url)
        except Exception:
            pass
        if httpserver.log:
            pytest.fail(f"SSRF vulnerability detected via redirect: {redirect_url}")

# Homoglyph
def test_ssrf_with_homoglyph_obfuscation():
    with HTTPServer() as httpserver:
        # Set up the server to respond with mock content
        httpserver.expect_request("/").respond_with_data("", content_type="application/json")

        server_port = httpserver.port

        # Homoglyph/Unicode obfuscation (full-width characters)
        # This would be "http://１２７.０.０.１" but it may not resolve properly
        homoglyph_payloads = [
            f"http://１２７.０.０.１:{server_port}/test",  # Full-width characters
        ]

        for payload in homoglyph_payloads:
            try:
                get_weather_from_provider(payload)
            except Exception:
                pass

            if httpserver.log:
                pytest.fail(f"SSRF vulnerability detected with homoglyph payload: {payload}")