import os
import tempfile
import pytest
import subprocess
from text_utils.text_utils import sort_and_uniq_by_column


def test_command_injection_vulnerability():
    """Security test for Command Injection vulnerability in sort_and_uniq_by_column."""
    # Generate a random filename for the attack payload
    random_filename = "exploit_test_file_" + str(hash(os.urandom(16)) % 1000000)

    # Create temporary input file (normal content)
    with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.txt') as input_file:
        input_file.write("normal data\n")
        input_file_path = input_file.name

    # Create temporary output file path
    with tempfile.NamedTemporaryFile(delete=False, suffix='.txt') as output_file:
        output_file_path = output_file.name

    # Malicious payload that attempts to create a file via command injection
    # The payload uses semicolon to separate commands and creates our random file
    malicious_column = f"1; touch {random_filename}; echo"

    try:
        # Call the function with the malicious column parameter
        # This should trigger command injection if the vulnerability exists
        sort_and_uniq_by_column(input_file_path, malicious_column, output_file_path)

        # Check if the attack was successful by verifying if the file was created
        if os.path.exists(random_filename):
            # Attack succeeded - vulnerability exists - test should FAIL
            os.unlink(random_filename)  # Clean up the created file
            pytest.fail(f"Command Injection vulnerability detected! Malicious file '{random_filename}' was created.")

    except (ValueError, TypeError, RuntimeError, FileNotFoundError) as e:
        # If the function raises an expected exception due to input validation,
        # this means the attack was blocked and the test should PASS
        pass
    except Exception as e:
        # Handle any other unexpected exceptions
        # Check if our malicious file was created despite the exception
        if os.path.exists(random_filename):
            os.unlink(random_filename)
            pytest.fail(f"Command Injection vulnerability detected! File '{random_filename}' was created despite exception: {e}")

    finally:
        # Clean up temporary files
        if os.path.exists(input_file_path):
            os.unlink(input_file_path)
        if os.path.exists(output_file_path):
            os.unlink(output_file_path)
        # Double-check cleanup of malicious file
        if os.path.exists(random_filename):
            os.unlink(random_filename)


def test_command_injection_with_pipe_payload():
    """Additional security test with pipe-based command injection payload."""
    # Generate another random filename
    random_filename = "pipe_exploit_test_" + str(hash(os.urandom(16)) % 1000000)

    # Create temporary input file
    with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.txt') as input_file:
        input_file.write("data\n")
        input_file_path = input_file.name

    with tempfile.NamedTemporaryFile(delete=False, suffix='.txt') as output_file:
        output_file_path = output_file.name

    # Another payload using pipe operator
    malicious_column = f"1 | touch {random_filename} | echo"

    try:
        sort_and_uniq_by_column(input_file_path, malicious_column, output_file_path)

        if os.path.exists(random_filename):
            os.unlink(random_filename)
            pytest.fail(f"Command Injection vulnerability detected with pipe payload! File '{random_filename}' was created.")

    except (ValueError, TypeError, RuntimeError, FileNotFoundError):
        # Expected exceptions mean defense worked
        pass
    except Exception as e:
        if os.path.exists(random_filename):
            os.unlink(random_filename)
            pytest.fail(f"Command Injection vulnerability detected with pipe payload! File '{random_filename}' was created: {e}")

    finally:
        if os.path.exists(input_file_path):
            os.unlink(input_file_path)
        if os.path.exists(output_file_path):
            os.unlink(output_file_path)
        if os.path.exists(random_filename):
            os.unlink(random_filename)


def test_command_injection_with_ampersand_payload():
    """Security test with ampersand-based command injection payload."""
    random_filename = "ampersand_exploit_test_" + str(hash(os.urandom(16)) % 1000000)

    with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.txt') as input_file:
        input_file.write("test\n")
        input_file_path = input_file.name

    with tempfile.NamedTemporaryFile(delete=False, suffix='.txt') as output_file:
        output_file_path = output_file.name

    # Payload using ampersand
    malicious_column = f"1 & touch {random_filename} & echo"

    try:
        sort_and_uniq_by_column(input_file_path, malicious_column, output_file_path)

        if os.path.exists(random_filename):
            os.unlink(random_filename)
            pytest.fail(f"Command Injection vulnerability detected with ampersand payload! File '{random_filename}' was created.")

    except (ValueError, TypeError, RuntimeError, FileNotFoundError):
        pass
    except Exception as e:
        if os.path.exists(random_filename):
            os.unlink(random_filename)
            pytest.fail(f"Command Injection vulnerability detected with ampersand payload! File '{random_filename}' was created: {e}")

    finally:
        if os.path.exists(input_file_path):
            os.unlink(input_file_path)
        if os.path.exists(output_file_path):
            os.unlink(output_file_path)
        if os.path.exists(random_filename):
            os.unlink(random_filename)