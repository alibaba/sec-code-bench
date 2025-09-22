# Copyright (c) 2025 Alibaba Group and its affiliates

# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at

#     http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import subprocess
from abc import abstractmethod

from sec_code_bench.editor.abstract import Editor
from sec_code_bench.utils.logger_utils import Logger

LOG = Logger.get_logger(__name__)


class CliEditor(Editor):
    """
    Abstract base class for CLI-based editors.

    This class provides functionality to run code generation through
    command-line interfaces in a separate thread with timeout support.
    """

    def __init__(self, timeout: int = 300) -> None:
        """
        Initialize the CliEditor instance.

        Args:
            timeout: Maximum time allowed for code generation in seconds (default: 300)
        """
        super().__init__(timeout)
        self.finish: bool = False
        self.return_code: int = 0
        self.std_out: str = ""
        self.std_err: str = ""

    def coding(
        self, code_dir: str, prompt: str, need_prepare: bool = False, debug: bool = False
    ) -> None:
        """
        Generate code based on the given prompt using CLI in a separate thread.

        Args:
            code_dir: The testcase code directory
            prompt: The prompt to guide code generation
            need_prepare: Whether preparation steps are needed (default: False)
            debug: Whether to enable debug mode for application type editors
                  (default: False)
        """
        # Execute CLI command directly without using an additional thread
        # This allows the outer ThreadPoolExecutor to handle concurrency
        self.run_cli(code_dir, prompt, need_prepare)

        if self.finish:
            if self.return_code != 0:
                LOG.error(
                    f"command failed (exit code {self.return_code}):\n{self.std_err}"
                )
            LOG.info(f"cli run: stdout: {self.std_out}")
        else:
            LOG.warning("CLI command did not complete properly")
        return

    def run_cli(self, code_dir: str, prompt: str, need_prepare: bool = False) -> None:
        """
        Execute the CLI command to generate code.

        Args:
            code_dir: The testcase code directory
            prompt: The prompt to guide code generation
            need_prepare: Whether preparation steps are needed (default: False)
        """
        # TODO: Prompt length is limited under shell commands
        command: list[str] = [
            self._get_binary_name(),
            self._get_prompt_args(),
            '"' + prompt + '"',
        ] + self._get_extends_args()

        proc: subprocess.Popen | None = None
        try:
            proc = subprocess.Popen(
                command,
                cwd=code_dir,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
            )
            self.std_out, self.std_err = proc.communicate(timeout=self.timeout)
            self.return_code = proc.returncode
        except subprocess.TimeoutExpired:
            # Process timed out, force termination
            if proc:
                proc.kill()
                self.std_out, self.std_err = proc.communicate()
                self.return_code = -1
                self.std_err = "Process timed out and was killed"
            else:
                self.return_code = -1
                self.std_err = "Process creation failed"
        except Exception as e:
            # Other exceptions
            if proc:
                proc.kill()
            self.return_code = -1
            self.std_err = f"Process execution failed: {str(e)}"
        finally:
            # Ensure process is cleaned up
            if proc and proc.poll() is None:
                proc.kill()
                proc.wait()

        self.finish = True

    def __enter__(self) -> "CliEditor":
        """
        Enter the runtime context.

        Returns:
            CliEditor instance
        """
        return self

    def __exit__(
        self,
        exc_type: type | None,
        exc_val: BaseException | None,
        exc_tb: object | None,
    ) -> bool | None:
        """
        Exit the runtime context.

        Args:
            exc_type: Exception type
            exc_val: Exception value
            exc_tb: Exception traceback

        Returns:
            Whether the exception was handled
        """
        self.close()
        return None

    def close(self) -> None:
        """
        Close the CLI editor and clean up resources.
        """
        # Reset state, prepare for next use
        self.finish = False
        self.return_code = 0
        self.std_out = ""
        self.std_err = ""

    @abstractmethod
    def _get_prompt_args(self) -> str:
        """
        Get the prompt arguments for the CLI command.

        Returns:
            Prompt arguments as string
        """
        pass

    @abstractmethod
    def _get_extends_args(self) -> list[str]:
        """
        Get the extended arguments for the CLI command.

        Returns:
            Extended arguments as list of strings
        """
        pass
