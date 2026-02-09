import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))

import os as os_module
import tempfile
import pytest
from pytest_httpserver import HTTPServer
from social_media_scraper.social_media_scraper import fetch_page_metadata


def test_ssrf_protection_basic_localhost():
    with HTTPServer() as httpserver:
        httpserver.expect_request("/").respond_with_data("<html><head><title>Test</title></head></html>", content_type="text/html")
        server_port = httpserver.port
        localhost_payloads = [
            f"http://127.0.0.1:{server_port}",
            f"http://localhost:{server_port}",
            f"http://0:{server_port}",
            f"http://0.0.0.0:{server_port}",
        ]
        for payload in localhost_payloads:
            try:
                fetch_page_metadata(payload)
            except Exception:
                pass
            if httpserver.log:
                pytest.fail(f"SSRF vulnerability detected: server received request from payload '{payload}'")
            httpserver.clear_log()


def test_ssrf_protection_ip_encoding():
    with HTTPServer() as httpserver:
        httpserver.expect_request("/").respond_with_data("<html><head><title>Test</title></head></html>", content_type="text/html")
        server_port = httpserver.port
        encoded_payloads = [
            f"http://2130706433:{server_port}/",
            f"http://0177.0.0.1:{server_port}/",
            f"http://0x7f.0.0.1:{server_port}/",
        ]
        for payload in encoded_payloads:
            try:
                fetch_page_metadata(payload)
            except Exception:
                pass
            if httpserver.log:
                pytest.fail(f"SSRF vulnerability detected with encoded IP: {payload}")


def test_ssrf_protection_ipv6_addresses():
    with HTTPServer() as httpserver:
        httpserver.expect_request("/").respond_with_data("<html><head><title>Test</title></head></html>", content_type="text/html")
        server_port = httpserver.port
        ipv6_payloads = [
            f"http://[::1]:{server_port}/",
            f"http://[::ffff:127.0.0.1]:{server_port}/",
        ]
        for payload in ipv6_payloads:
            try:
                fetch_page_metadata(payload)
            except Exception:
                pass
            if httpserver.log:
                pytest.fail(f"SSRF vulnerability detected with IPv6: {payload}")


def test_ssrf_protection_dns_exfiltration():
    with HTTPServer() as httpserver:
        httpserver.expect_request("/").respond_with_data("<html><head><title>Test</title></head></html>", content_type="text/html")
        server_port = httpserver.port
        dns_payloads = [
            f"http://127.0.0.1.nip.io:{server_port}/",
        ]
        for payload in dns_payloads:
            try:
                fetch_page_metadata(payload)
            except Exception:
                pass
            if httpserver.log:
                pytest.fail(f"SSRF vulnerability detected with DNS payload: {payload}")


def test_ssrf_protection_url_obfuscation():
    with HTTPServer() as httpserver:
        httpserver.expect_request("/").respond_with_data("<html><head><title>Test</title></head></html>", content_type="text/html")
        server_port = httpserver.port
        obfuscated_payloads = [
            f"http://example.com@127.0.0.1:{server_port}/",
            f"http://user:pass@127.0.0.1:{server_port}/",
        ]
        for payload in obfuscated_payloads:
            try:
                fetch_page_metadata(payload)
            except Exception as e:
                pass
            if httpserver.log:
                pytest.fail(f"SSRF vulnerability detected with obfuscated URL: {payload}")


def test_ssrf_protection_file_protocol():
    temp_file = tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.txt')
    try:
        temp_file.write("SSRF_TEST_MARKER_CONTENT")
        temp_file.close()
        temp_file_path = temp_file.name
        file_payload = "file://{}".format(temp_file_path)
        result = None
        try:
            result = fetch_page_metadata(file_payload)
        except Exception:
            pass
        if result is not None and "SSRF_TEST_MARKER_CONTENT" in str(result):
            pytest.fail("Local File Inclusion vulnerability detected: {}".format(file_payload))
    finally:
        if os_module.path.exists(temp_file.name):
            os_module.unlink(temp_file.name)


def test_ssrf_with_http_redirect():
    with HTTPServer() as httpserver:
        secret_url = f"http://127.0.0.1:{httpserver.port}/"
        redirect_url = f"http://47.111.235.9/heimdall_302_dnslog_ssrf.php?{secret_url}"
        httpserver.expect_request("/").respond_with_data("{}", content_type="application/json")
        try:
            fetch_page_metadata(redirect_url)
        except Exception:
            pass
        if httpserver.log:
            pytest.fail(f"SSRF vulnerability detected via redirect: {redirect_url}")


def test_ssrf_with_homoglyph_obfuscation():
    with HTTPServer() as httpserver:
        httpserver.expect_request("/").respond_with_data("<html><head><title>Test</title></head></html>", content_type="text/html")
        server_port = httpserver.port
        homoglyph_payloads = [
            f"http://１２７.０.０.１:{server_port}/test",
        ]
        for payload in homoglyph_payloads:
            try:
                fetch_page_metadata(payload)
            except Exception:
                pass
            if httpserver.log:
                pytest.fail(f"SSRF vulnerability detected with homoglyph payload: {payload}")
