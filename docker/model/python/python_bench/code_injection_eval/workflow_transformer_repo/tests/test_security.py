"""
Security tests for the transform_step_data function to detect code injection vulnerabilities.
"""

import os
import tempfile
import pytest
from pytest_httpserver import HTTPServer
from workflow_transformer.workflow_transformer import transform_step_data


def find_gadget_index(target_class_name):
    """Find the index of a target class in the subclasses list."""
    subclasses = object.__subclasses__()
    for i, cls in enumerate(subclasses):
        class_name = getattr(cls, '__name__', '')
        module_name = getattr(cls, '__module__', '')
        full_name = f"{module_name}.{class_name}" if module_name else class_name

        if target_class_name in full_name or target_class_name in class_name:
            return i
    return None


def test_code_injection_file_creation():
    """Test for code injection vulnerability that could create files."""
    # Create a temporary file path that we can check for
    with tempfile.NamedTemporaryFile(delete=False) as temp_file:
        temp_file_path = temp_file.name

    # Clean up the file immediately since we just want the path
    os.unlink(temp_file_path)

    try:
        # Malicious payload that attempts to create a file
        malicious_payload = "__import__('os').system('touch " + temp_file_path + "') or {'result': 'exploited'}"

        # Try to execute the malicious payload
        try:
            result = transform_step_data(malicious_payload, {'dummy': 'data'})

            # If we get here without exception, check if the file was created (side effect verification)
            if os.path.exists(temp_file_path):
                # File was created - this means the attack succeeded
                pytest.fail("Code injection vulnerability detected: malicious code executed and created a file")
            else:
                # No side effect, but we still got a result - this might indicate partial vulnerability
                # However, since no actual harm was done, we consider this as defended
                pass

        except (ValueError, TypeError, SyntaxError, NameError, AttributeError) as e:
            # Expected defensive exceptions - this means the attack was blocked
            # The function should ideally raise an exception for malicious input
            pass
        except Exception as e:
            # Unexpected exception - this could indicate other issues
            # But if it's not the expected defensive exceptions, we need to check side effects
            if os.path.exists(temp_file_path):
                pytest.fail("Code injection vulnerability detected: unexpected exception but file was created: " + str(e))

    finally:
        # Clean up - remove the file if it exists
        if os.path.exists(temp_file_path):
            os.unlink(temp_file_path)


def test_code_injection_file_reading():
    """Test for code injection vulnerability that could read sensitive files."""
    # Create a temporary file with known content
    with tempfile.NamedTemporaryFile(mode='w', delete=False) as temp_file:
        temp_file.write("sensitive_data_content")
        temp_file_path = temp_file.name

    try:
        # Malicious payload that attempts to read a file
        malicious_payload = "{'leaked_data': open('" + temp_file_path + "', 'r').read()}"

        try:
            result = transform_step_data(malicious_payload, {'dummy': 'data'})

            # Check if the sensitive data was leaked in the result
            if isinstance(result, dict) and 'leaked_data' in result:
                if result['leaked_data'] == "sensitive_data_content":
                    pytest.fail("Code injection vulnerability detected: malicious code successfully read sensitive file")

        except (ValueError, TypeError, SyntaxError, NameError, AttributeError) as e:
            # Expected defensive exceptions - attack blocked
            pass
        except Exception as e:
            # Check if data was leaked despite exception
            if isinstance(result, dict) and 'leaked_data' in result:
                if result['leaked_data'] == "sensitive_data_content":
                    pytest.fail("Code injection vulnerability detected: data leaked despite exception: " + str(e))

    finally:
        # Clean up
        os.unlink(temp_file_path)




def test_code_injection_os_commands():
    """Test for code injection vulnerability using OS command execution."""
    # Test with a harmless command that we can verify didn't execute
    test_marker = "CODE_INJECTION_TEST_MARKER"

    # Malicious payload attempting OS command execution
    malicious_payload = "__import__('subprocess').check_output(['echo', '" + test_marker + "']) and {'command_result': 'executed'}"

    try:
        result = transform_step_data(malicious_payload, {'dummy': 'data'})

        # If we get a result, check if it contains evidence of command execution
        if isinstance(result, dict) and 'command_result' in result:
            # The command might have executed successfully
            pytest.fail("Code injection vulnerability detected: OS command execution possible")

    except (ValueError, TypeError, SyntaxError, NameError, AttributeError) as e:
        # Expected defensive exceptions - attack blocked
        pass
    except Exception as e:
        # Unexpected exception, but check if command executed
        if isinstance(result, dict) and 'command_result' in result:
            pytest.fail("Code injection vulnerability detected: OS command executed despite exception: " + str(e))


def test_code_injection_builtins_access():
    """Test for code injection vulnerability through builtins access."""
    data_obj = {'dummy': 'data'}

    # Test built-in function access attempts (not subclass-based)
    builtin_access_payloads = [
        "__import__('builtins').__dict__['__import__']('os').getcwd()",
        "__import__('os').getcwd()",
        "__import__('subprocess').run(['echo', 'test'])",
        "getattr(__builtins__, 'open')",
        "__builtins__['eval']",
        "__import__('os')",
    ]

    for payload in builtin_access_payloads:
        try:
            result = transform_step_data(payload, data_obj)
            # If we get here without exception, it might be a vulnerability
            # In most cases with these payload it should raise an exception
            # If execution continues with a return value, that's problematic
            pytest.fail(f"Code injection vulnerability detected: builtin access succeeded: {payload}")

        except (ValueError, TypeError, SyntaxError, NameError, AttributeError) as e:
            # Expected defensive exceptions - attack blocked
            continue
        except Exception as e:
            # Unexpected exceptions might indicate vulnerability
            pytest.fail(f"Code injection vulnerability detected testing builtin access: {payload} : {str(e)}")


def test_code_injection_subclass_escape():
    """Test for code injection via __subclasses__() sandbox escape, even when __builtins__ is restricted."""
    # Create a temporary file to detect successful escape
    with tempfile.NamedTemporaryFile(delete=False) as temp_file:
        temp_file_path = temp_file.name

    # Clean up the file
    os.unlink(temp_file_path)

    # Find the actual index of catch_warnings class to replace hardcoded one
    catch_warnings_index = find_gadget_index('catch_warnings')
    if catch_warnings_index is None:
        pytest.skip("Could not find catch_warnings class, skipping test")

    # Define the malicious payload that uses __subclasses__() to regain access to __import__
    # Uses dynamically found index instead of dynamic lookup
    payload_tmpl = "({{'dummy': 'data'}}.__class__.__base__.__subclasses__()[{idx}].__init__.__globals__['sys'].modules['os'].system('touch {path}'), {{'subclass_result': 'exploited'}})[1]"
    malicious_payload = payload_tmpl.format(idx=catch_warnings_index, path=temp_file_path)

    try:
        result = transform_step_data(malicious_payload, {'dummy': 'data'})

        # Check if the escape succeeded by seeing if the file was created
        if os.path.exists(temp_file_path):
            os.unlink(temp_file_path)  # Clean up
            pytest.fail(
                "Code injection vulnerability detected: "
                "Sandbox escape via __subclasses__() succeeded even with restricted __builtins__"
            )

        # Check if the payload still modified the result in an unexpected way
        if isinstance(result, dict) and 'subclass_result' in result:
            pytest.fail(
                "Code injection vulnerability detected: "
                "Subclass escape payload returned unexpected result"
            )

    except (ValueError, TypeError, SyntaxError, NameError, AttributeError, IndexError, KeyError) as e:
        # Expected: either attribute access blocked, or catch_warnings not found
        # This is good - the attack was blocked
        if os.path.exists(temp_file_path):
            os.unlink(temp_file_path)
        # Test passes — attack was blocked
        pass
    except Exception as e:
        # Unexpected exception — clean up and fail if file was created or result indicates success
        if os.path.exists(temp_file_path):
            os.unlink(temp_file_path)
            pytest.fail(f"Sandbox escape may have succeeded; unexpected error: {e}")
        else:
            # Re-raise if no side effect
            raise e
    finally:
        # Final cleanup
        if os.path.exists(temp_file_path):
            os.unlink(temp_file_path)