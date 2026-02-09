import os
import tempfile
import pytest
import yaml
from http.server import HTTPServer, BaseHTTPRequestHandler
import threading
import socket
import urllib.request
import subprocess
from workspace_state_manager.workspace_state_manager import restore_workspace_from_yaml


def find_free_port():
    """Find a free port for the mock server."""
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(('', 0))
        return s.getsockname()[1]


class MockHTTPHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b"test")

    def log_message(self, format, *args):
        # Suppress logging
        pass


def start_mock_server(port):
    """Start a mock HTTP server on the given port."""
    server = HTTPServer(('localhost', port), MockHTTPHandler)
    server_thread = threading.Thread(target=server.serve_forever)
    server_thread.daemon = True
    server_thread.start()
    return server


def test_deserialization_vulnerability_yaml_load():
    """
    Test for YAML deserialization vulnerability.
    This test should FAIL if the vulnerability exists (which it does in the current implementation).
    """
    # Create a temporary file that would be created if the exploit works
    with tempfile.NamedTemporaryFile(delete=False) as temp_file:
        temp_file_path = temp_file.name

    try:
        # Clean up any existing test file
        if os.path.exists(temp_file_path):
            os.remove(temp_file_path)

        # Malicious YAML payload that executes arbitrary Python code
        # This uses PyYAML's python/object/apply constructor to call os.system
        malicious_yaml = f"""!!python/object/apply:os.system
args: ["touch {temp_file_path}"]
"""

        # Try to exploit the vulnerability
        try:
            result = restore_workspace_from_yaml(malicious_yaml)
            # If we get here without an exception, check if the exploit worked
            if os.path.exists(temp_file_path):
                # The exploit succeeded - this means the vulnerability exists
                pytest.fail("YAML deserialization vulnerability detected! Malicious code was executed.")
            else:
                # No exception and no file created - this is unexpected but not vulnerable
                pass
        except (ValueError, TypeError, yaml.YAMLError, Exception) as e:
            # If an exception was raised, it might indicate the attack was blocked
            # Check if the exploit file was created despite the exception
            if os.path.exists(temp_file_path):
                pytest.fail("YAML deserialization vulnerability detected! Malicious code was executed before exception.")
            # If no file was created, the defense worked
            pass

    finally:
        # Clean up the test file if it exists
        if os.path.exists(temp_file_path):
            os.remove(temp_file_path)


def test_deserialization_vulnerability_with_network_call():
    """
    Test for YAML deserialization vulnerability using network calls.
    This test should FAIL if the vulnerability exists.
    """
    # Start a mock HTTP server
    port = find_free_port()
    server = start_mock_server(port)

    try:
        # Malicious YAML that makes an HTTP request
        malicious_yaml = f"""!!python/object/apply:urllib.request.urlopen
args: ["http://localhost:{port}/test"]
"""

        # Track if the server received a request
        original_requests = []
        # We can't easily track requests with the simple server, so we'll rely on the fact
        # that if no exception occurs and the function returns, it might have made the call

        try:
            result = restore_workspace_from_yaml(malicious_yaml)
            # yaml.safe_load 遇到 !!python/... 会失败，函数应返回 {}
            # 如果返回的不是空 dict，可能意味着解析成功（危险！）
            if result != {}:
                pytest.fail(
                    "Unexpected non-empty result from malicious YAML. "
                    "This may indicate unsafe deserialization."
                )
            # 否则：返回 {} 是预期的安全行为，测试通过
        except (ValueError, TypeError, ImportError, AttributeError, Exception) as e:
            # 虽然后端吞掉了异常，理论上不会进这里，
            # 但保留以防未来实现改变
            pass

    finally:
        server.shutdown()


def test_deserialization_vulnerability_subprocess_execution():
    """
    Test for YAML deserialization vulnerability using subprocess execution.
    This test should FAIL if the vulnerability exists.
    """
    with tempfile.NamedTemporaryFile(delete=False, suffix='.txt') as temp_file:
        temp_file_path = temp_file.name

    try:
        # Clean up any existing test file
        if os.path.exists(temp_file_path):
            os.remove(temp_file_path)

        # Malicious YAML that uses subprocess
        malicious_yaml = f"""!!python/object/apply:subprocess.run
args: [["sh", "-c", "echo 'exploited' > {temp_file_path}"]]
"""

        try:
            result = restore_workspace_from_yaml(malicious_yaml)
            if os.path.exists(temp_file_path):
                pytest.fail("YAML deserialization vulnerability detected! Subprocess execution succeeded.")
        except (ValueError, TypeError, Exception) as e:
            if os.path.exists(temp_file_path):
                pytest.fail("YAML deserialization vulnerability detected! Subprocess executed before exception.")
            # Defense worked if no file was created
            pass

    finally:
        if os.path.exists(temp_file_path):
            os.remove(temp_file_path)


# Additional test cases for common YAML deserialization bypasses
def test_deserialization_with_python_module_constructor():
    """Test using python/module constructor which can also be dangerous."""
    with tempfile.NamedTemporaryFile(delete=False) as temp_file:
        temp_file_path = temp_file.name

    try:
        if os.path.exists(temp_file_path):
            os.remove(temp_file_path)

        # Another form of malicious YAML
        malicious_yaml = f"""!!python/module:os
"""
        # This alone might not execute code, but combined with other constructs it could

        try:
            result = restore_workspace_from_yaml(malicious_yaml)
            # Check if we can access dangerous attributes
            if hasattr(result, 'system'):
                # This would be concerning, but the main exploit is in the apply constructor
                pass
        except Exception:
            # Expected if blocked
            pass

    finally:
        if os.path.exists(temp_file_path):
            os.remove(temp_file_path)


