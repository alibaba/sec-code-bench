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

from enum import Enum

from sec_code_bench.editor.abstract import Editor
from sec_code_bench.editor.application.buddy import CodeBuddyEditor
from sec_code_bench.editor.application.cursor import CursorEditor
from sec_code_bench.editor.application.vscode_copilot import GitHubCopilotEditor
from sec_code_bench.editor.application.lingma import LingMaEditor
from sec_code_bench.editor.application.qoder import QoderEditor
from sec_code_bench.editor.application.trae import TraeEditor
from sec_code_bench.editor.application.vscode_buddy import VscodeBuddyEditor
from sec_code_bench.editor.application.vscode_lingma import VscodeLingmaEditor
from sec_code_bench.editor.application.vscode_zulu import VscodeZuluEditor
from sec_code_bench.editor.cli.claude_code import ClaudeCodeEditor
from sec_code_bench.editor.cli.codebuddy import CodeBuddyCliEditor
from sec_code_bench.editor.cli.codex import CodexEditor
from sec_code_bench.editor.cli.qwen_code import QwenEditor


class IDEModel(Enum):
    """Enumeration of IDE models."""

    CLI = "cli"
    APP = "app"


class IDEType(Enum):
    """Enumeration of supported IDE types."""

    VSCODE_LINGMA = "vscode-lingma"
    VSCODE_BUDDY = "vscode-buddy"
    VSCODE_ZULU = "vscode-zulu"
    VSCODE_GITHUB_COPILOT = "vscode-copilot"
    LINGMA = "lingma"
    CURSOR = "cursor"
    TRAE = "trae"
    QODER = "qoder"
    CodeBuddy = "code-buddy"

    CLAUDE_CODE = "claude-code"
    CODEBUDDY_CLI = "codebuddy-cli"
    QWEN_CODE = "qwen-code"
    CODEX = "codex"


class EditorFactory:
    """Factory class for creating editor instances."""

    editors: dict[IDEType, type[Editor]] = {
        # APP Type
        IDEType.VSCODE_LINGMA: VscodeLingmaEditor,
        IDEType.VSCODE_BUDDY: VscodeBuddyEditor,
        IDEType.VSCODE_ZULU: VscodeZuluEditor,
        IDEType.VSCODE_GITHUB_COPILOT: GitHubCopilotEditor,
        IDEType.CURSOR: CursorEditor,
        IDEType.TRAE: TraeEditor,
        IDEType.QODER: QoderEditor,
        IDEType.LINGMA: LingMaEditor,
        IDEType.CodeBuddy: CodeBuddyEditor,
        # CLI type
        IDEType.CLAUDE_CODE: ClaudeCodeEditor,
        IDEType.CODEBUDDY_CLI: CodeBuddyCliEditor,
        IDEType.QWEN_CODE: QwenEditor,
        IDEType.CODEX: CodexEditor,
    }

    @classmethod
    def get_editor(cls, editor_type: str) -> Editor:
        """
        Get an editor instance based on the editor type.

        Args:
            editor_type: Type of editor to create

        Returns:
            Editor instance
        """
        return cls.editors[IDEType(editor_type)]()
