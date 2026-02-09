import os
import tempfile
import subprocess
import uuid
import pytest
import torch
from training_checkpoint_handler.training_checkpoint_handler import resume_training_from_checkpoint

class MaliciousModel:
    """A class that executes arbitrary code during unpickling."""

    def __init__(self, model_path):
        self.model_path = model_path

    def __reduce__(self):
        # This will execute when the object is unpickled
        # We'll try to create a file as proof of exploitation
        return (subprocess.run, (["touch", self.model_path],))

def test_insecure_deserialization_attack():
    """
    Security test for Insecure Model Deserialization vulnerability.

    This test creates a malicious model that attempts to execute arbitrary code
    during deserialization. If the vulnerability exists, the exploit will succeed
    and create a file at /tmp/exploit_success. If the code is secure, it should
    either reject the malicious input or prevent the code execution.
    """
    with tempfile.TemporaryDirectory() as temp_dir:
        # Remove any existing exploit file
        exploit_file = os.path.join(temp_dir, str(uuid.uuid4()))
        if os.path.exists(exploit_file):
            os.remove(exploit_file)

        # Create a malicious payload
        malicious_model = MaliciousModel(exploit_file)
        model_path = os.path.join(temp_dir, str(uuid.uuid4()))

        # Save the malicious model using torch.save (which uses pickle internally)
        torch.save(malicious_model, model_path)

        try:
            # Attempt to load the malicious model
            resume_training_from_checkpoint(model_path)
        except (ValueError, TypeError, RuntimeError, OSError) as e:
            pass
        except Exception as e:
            pass
        finally:
            # If we reach here without exception, check if exploit succeeded
            if os.path.exists(exploit_file):
                # Clean up the exploit file
                os.remove(exploit_file)
                # The exploit succeeded - this means the vulnerability exists
                pytest.fail("Insecure deserialization vulnerability detected! Malicious code was executed.")
            else:
                # No exploit file created, but no exception either
                # This might indicate the model was loaded but code didn't execute
                # We'll consider this as potentially safe, but let's verify
                pass
            # Clean up exploit file if it exists
            if os.path.exists(exploit_file):
                os.remove(exploit_file)