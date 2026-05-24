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

from sec_code_bench.editor.cli import CliEditor


class ClaudeCodeEditor(CliEditor):
    """Claude CLI editor implementation for code generation."""

    def _get_prompt_args(self) -> str:
        """
        Get the prompt arguments for the Claude CLI command.

        Returns:
            Prompt argument flag as string
        """
        return "-p"

    def _get_binary_name(self) -> str:
        """
        Get the name of the Claude binary.

        Returns:
            Binary name as string
        """
        return "claude"

    def get_editor(self) -> str:
        """
        Get the editor name.

        Returns:
            Editor name as string
        """
        return "claude-code"

    def get_type(self) -> str:
        """
        Get the editor type.

        Returns:
            Editor type as string
        """
        return "cli"

    def _get_extends_args(self) -> list[str]:
        """
        Get the extended arguments for the Claude CLI command.

        Returns:
            Extended arguments as list of strings
        """
        args: list[str] = []
        if self.model_config.api_key:
            args.extend(["--setting-sources", "project,local"])
        args.append("--dangerously-skip-permissions")
        return args

    def _get_env(self) -> dict[str, str]:
        """
        Build a Claude Code environment from the current process while removing
        auth settings that conflict with an explicitly supplied API key.
        """
        env = super()._get_env()

        if self.model_config.api_key:
            # Claude Code can load these from user settings or the shell and
            # send an Authorization header alongside x-api-key. DashScope's
            # Anthropic-compatible API rejects requests that contain both.
            env.pop("ANTHROPIC_AUTH_TOKEN", None)
            env.pop("ANTHROPIC_CUSTOM_HEADERS", None)
            env.pop("CLAUDE_CODE_OAUTH_TOKEN", None)
            env["ANTHROPIC_API_KEY"] = self.model_config.api_key

        if self.model_config.base_url:
            env["ANTHROPIC_BASE_URL"] = self.model_config.base_url

        return env

    def _get_config_env(self) -> dict[str, str]:
        """
        Claude Code supports --model directly; API credentials/base URL are
        provided through Anthropic-compatible environment variables.
        """
        env: dict[str, str] = {}
        if self.model_config.api_key:
            env["ANTHROPIC_API_KEY"] = self.model_config.api_key
        if self.model_config.base_url:
            env["ANTHROPIC_BASE_URL"] = self.model_config.base_url
        return env
