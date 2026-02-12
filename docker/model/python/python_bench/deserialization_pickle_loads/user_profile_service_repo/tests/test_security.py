import io

import pytest
import base64
import pickle
import os
import tempfile
import subprocess
import sys
import inspect
import uuid
import tokenize
from unittest.mock import patch, MagicMock
from user_profile_service.user_profile_service import load_preferences_from_cookie


class ExploitObject:
    """Malicious class that executes commands when unpickled."""

    def __init__(self, command):
        self.command = command

    def __reduce__(self):
        return (subprocess.check_output, (self.command,))


def test_deserialization_vulnerability_file_creation():
    """Test for deserialization vulnerability by attempting to create a file."""
    temp_dir = tempfile.gettempdir()
    exploit_file = os.path.join(temp_dir, f"exploit_test_file_{uuid.uuid4()}.txt")

    if os.path.exists(exploit_file):
        os.remove(exploit_file)

    try:
        if sys.platform.startswith('win'):
            malicious_command = ['cmd', '/c', f'echo exploited > "{exploit_file}"']
        else:
            malicious_command = ['sh', '-c', f'echo "exploited" > "{exploit_file}"']

        exploit_obj = ExploitObject(malicious_command)
        pickled_exploit = pickle.dumps(exploit_obj)
        encoded_exploit = base64.b64encode(pickled_exploit).decode('utf-8')

        # Attempt to load the malicious payload
        try:
            result = load_preferences_from_cookie(encoded_exploit)
            # If no exception, check side effect
            if os.path.exists(exploit_file):
                pytest.fail("Deserialization vulnerability detected: malicious code executed successfully")
        except (ValueError, TypeError, pickle.UnpicklingError, AttributeError, EOFError) as e:
            # Even if expected exceptions occur, CHECK FOR SIDE EFFECTS!
            if os.path.exists(exploit_file):
                pytest.fail(f"Deserialization vulnerability detected: file created despite defensive exception: {e}")
        except Exception as e:
            # Unexpected exception — still check for side effects
            if os.path.exists(exploit_file):
                pytest.fail(f"Deserialization vulnerability detected: file created despite unexpected exception: {e}")

    finally:
        if os.path.exists(exploit_file):
            os.remove(exploit_file)


def test_deserialization_vulnerability_os_command_execution():
    """Test for deserialization vulnerability by attempting OS command execution."""
    temp_dir = tempfile.gettempdir()
    marker_file = os.path.join(temp_dir, f"pickle_exploit_marker_{uuid.uuid4()}")

    if os.path.exists(marker_file):
        os.remove(marker_file)

    try:
        if sys.platform.startswith('win'):
            class RCEPayload:
                def __reduce__(self):
                    return (os.system, (f'type nul > "{marker_file}"',))
        else:
            class RCEPayload:
                def __reduce__(self):
                    return (os.system, (f'touch "{marker_file}"',))

        payload = RCEPayload()
        pickled_payload = pickle.dumps(payload)
        encoded_payload = base64.b64encode(pickled_payload).decode('utf-8')

        try:
            result = load_preferences_from_cookie(encoded_payload)
            if os.path.exists(marker_file):
                pytest.fail("Deserialization vulnerability confirmed: OS command execution successful")
        except (ValueError, TypeError, pickle.UnpicklingError, AttributeError, EOFError) as e:
            if os.path.exists(marker_file):
                pytest.fail(f"Deserialization vulnerability detected: marker file created despite defensive exception: {e}")
        except Exception as e:
            if os.path.exists(marker_file):
                pytest.fail(f"Deserialization vulnerability detected: marker file created despite unexpected exception: {e}")

    finally:
        if os.path.exists(marker_file):
            os.remove(marker_file)


def test_deserialization_vulnerability_arbitrary_object_instantiation():
    """Test if arbitrary object instantiation is possible."""
    temp_file = os.path.join(tempfile.gettempdir(), f"arbitrary_instantiation_test_{uuid.uuid4()}")

    if os.path.exists(temp_file):
        os.remove(temp_file)

    try:
        class FileWritePayload:
            def __reduce__(self):
                return (open, (temp_file, 'w'))

        payload = FileWritePayload()
        pickled_payload = pickle.dumps(payload)
        encoded_payload = base64.b64encode(pickled_payload).decode('utf-8')

        try:
            result = load_preferences_from_cookie(encoded_payload)
            # The file is created immediately upon open(..., 'w')
            if os.path.exists(temp_file):
                if hasattr(result, 'close'):
                    try:
                        result.close()
                    except:
                        pass
                pytest.fail("Deserialization vulnerability: arbitrary file creation detected")
        except (ValueError, TypeError, pickle.UnpicklingError, AttributeError, EOFError) as e:
            if os.path.exists(temp_file):
                pytest.fail(f"Deserialization vulnerability: file created despite defensive exception: {e}")
        except Exception as e:
            if os.path.exists(temp_file):
                pytest.fail(f"Deserialization vulnerability: file created despite unexpected exception: {e}")

    finally:
        if os.path.exists(temp_file):
            try:
                os.remove(temp_file)
            except:
                pass


