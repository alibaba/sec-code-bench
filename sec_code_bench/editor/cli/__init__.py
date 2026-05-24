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

import os
import subprocess
from abc import abstractmethod
from dataclasses import dataclass, field

from sec_code_bench.editor.abstract import Editor
from sec_code_bench.utils.logger_utils import Logger

LOG = Logger.get_logger(__name__)


@dataclass(frozen=True)
class CliModelConfig:
    """Runtime model/provider settings passed to a CLI editor invocation."""

    model: str | None = None
    api_key: str | None = None
    base_url: str | None = None
    binary: str | None = None
    extra_args: tuple[str, ...] = field(default_factory=tuple)


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
        self.model_config = CliModelConfig()

    def set_model_config(self, model_config: CliModelConfig) -> None:
        """
        Set runtime model/provider settings for this CLI editor.

        Args:
            model_config: Model/API/base-url settings parsed from e2e arguments.
        """
        self.model_config = model_config

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
        command: list[str] = [self.model_config.binary or self._get_binary_name()]
        command.extend(self._get_config_args())
        command.extend(self._get_extends_args())
        prompt_arg = self._get_prompt_args()
        if prompt_arg:
            command.append(prompt_arg)
        command.append(prompt)
        env = self._get_env()

        # Log the complete command details for debugging
        LOG.info(f"CLI binary: {command[0]}")
        LOG.info(
            f"CLI args: {' '.join(self._mask_sensitive_args(command[1:]))}"
        )
        LOG.info(f"CLI working directory: {code_dir}")
        LOG.info(f"CLI full prompt:\n{prompt}\n--- END OF PROMPT ---")

        proc: subprocess.Popen | None = None
        try:
            proc = subprocess.Popen(
                command,
                cwd=code_dir,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                env=env,
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

        # Log command completion and check if files were created
        LOG.info(f"CLI finished with return_code={self.return_code}")
        # List files in code_dir to see what was created
        if os.path.isdir(code_dir):
            files_created = []
            for root, dirs, files in os.walk(code_dir):
                for f in files:
                    rel_path = os.path.relpath(os.path.join(root, f), code_dir)
                    files_created.append(rel_path)
            LOG.info(f"Files in code_dir after CLI: {files_created if files_created else '(empty)'}")

    def _get_config_args(self) -> list[str]:
        """
        Build CLI arguments for model/provider settings.

        Subclasses override this when a tool uses non-standard flag names.
        """
        args: list[str] = []
        if self.model_config.model:
            args.extend(["--model", self.model_config.model])
        args.extend(self.model_config.extra_args)
        return args

    def _get_env(self) -> dict[str, str]:
        """
        Build the environment for the child CLI process.

        Subclasses can map API settings to the environment when the tool does
        not expose stable command-line flags for them.
        """
        env = os.environ.copy()
        env.update(self._get_config_env())
        return env

    def _get_config_env(self) -> dict[str, str]:
        """
        Return environment variables derived from model/provider settings.

        Default implementation is empty because env var names are tool-specific.
        """
        return {}

    def _mask_sensitive_args(self, args: list[str]) -> list[str]:
        """Mask sensitive values before logging CLI arguments."""
        sensitive_flags = {
            "--api-key",
            "--apikey",
            "--openai-api-key",
            "--anthropic-api-key",
            "--gemini-api-key",
        }
        masked: list[str] = []
        skip_next = False
        for arg in args:
            if skip_next:
                masked.append("***")
                skip_next = False
                continue
            if arg in sensitive_flags:
                masked.append(arg)
                skip_next = True
                continue
            if any(token in arg.lower() for token in ("api-key=", "apikey=")):
                key, _, _ = arg.partition("=")
                masked.append(f"{key}=***")
                continue
            masked.append(arg)
        return masked

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
