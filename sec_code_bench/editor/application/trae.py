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

from sec_code_bench.editor.application import IdeEditor


class TraeEditor(IdeEditor):
    """Trae IDE editor implementation."""

    def _get_binary_name(self) -> str:
        """
        Get the name of the Trae binary.

        Returns:
            Binary name as string
        """
        return "trae"

    def _get_editor(self) -> str:
        """
        Get the editor name.

        Returns:
            Editor name as string
        """

    def get_editor(self):
        return "trae"

    def get_type(self) -> str:
        """
        Get the editor type.

        Returns:
            Editor type as string
        """
        return "embed"

    def _get_pages_selector(self) -> tuple[str, str]:
        """
        Get the pages selector for Trae IDE.

        Returns:
            Tuple of (JS script, selector)
        """
        return (
            """
        (() => {
           const pageContent = document.documentElement.outerHTML;
           return pageContent.indexOf("您正在与 Builder 聊天") !== -1;
       })();""",
            "您正在与 Builder 聊天",
        )

    def _get_focus_sign(self) -> tuple[str, str]:
        """
        Get the focus sign for Trae IDE.

        Returns:
            Tuple of (JS script, target selector)
        """
        return (
            """
        (() => {
           return true;
       })();""",
            "",
        )

    def _get_finish_sign(self) -> tuple[str, str]:
        """
        Get the finish sign for Trae IDE.

        Returns:
            Tuple of (JS script, flag)
        """
        result_check_js = """
        (() => {
            const button = document.querySelector('button[aria-label="重试"]');

            if (button) {
              if (button.disabled) {
                return 'not';
              } else {
                return 'finish';
              }
            } else {
              return 'no-button';
            }
        })();
        """
        return result_check_js, "finish"
