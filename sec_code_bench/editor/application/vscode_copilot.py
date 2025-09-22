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


class GitHubCopilotEditor(IdeEditor):
    """GitHub Copilot VS Code extension editor implementation."""

    def _get_binary_name(self) -> str:
        """
        Get the name of the VS Code binary.

        Returns:
            Binary name as string
        """
        return "code"

    def get_editor(self) -> str:
        """
        Get the editor name.

        Returns:
            Editor name as string
        """
        return "Visual Studio Code"

    def get_type(self) -> str:
        """
        Get the editor type.

        Returns:
            Editor type as string
        """
        return "embed"

    def _get_pages_selector(self) -> tuple[str, str]:
        """
        Get the pages selector for GitHub Copilot VS Code extension.
        Reference Cursor's approach: look for specific input elements.

        Returns:
            Tuple of (JS script, selector)
        """
        js_script = f"""
            (() => {{
                const bodyText = document.body ? document.body.textContent : '';
                const containsChinese = bodyText.includes('使用代理模式生成');
                const containsOtherChinese = bodyText.includes('让我们开始吧');
                const containsEnglish = bodyText.includes('Build with agent mode');
                return containsChinese || containsOtherChinese || containsEnglish;
            }})();
           """
        return js_script, ""

    def _get_focus_sign(self) -> tuple[str, str]:
        """
        Get the focus sign for GitHub Copilot VS Code extension.
        Reference Cursor's approach: click on the right panel first, then focus on input.

        Returns:
            Tuple of (JS script, target selector)
        """
        target_selector = ".interactive-input-part"
        focus_js = f"""
               (() => {{
                    const editorContainer = document.querySelector('.interactive-input-editor');
                      if (!editorContainer) {{
                        return false;
                      }}
                      const monacoEditor = editorContainer.querySelector('.monaco-editor');
                      if (!monacoEditor) {{
                        return false;
                      }}
                      const nativeEditContext = monacoEditor.querySelector('.native-edit-context');
                      if (!nativeEditContext) {{
                        return false;
                      }}
                      const viewLine = monacoEditor.querySelector('.view-line');
                      if (!viewLine) {{
                        return false;
                      }}
                      monacoEditor.classList.add('focused');
                      editorContainer.classList.add('focused');
                      nativeEditContext.focus();
                      return true;
               }})();
               """
        return focus_js, target_selector

    def _get_finish_sign(self) -> tuple[str, str]:
        """
        Get the finish sign for GitHub Copilot VS Code extension.

        Returns:
            Tuple of (JS script, flag)
        """
        js_script = """
           (() => {
                const hasSpanChanged = Array.from(document.querySelectorAll('span')).some(span => {
                    const text = span.textContent.trim();
                    return text.includes('已更改');
                });
               const pageContent = document.documentElement.outerHTML;
               if(hasSpanChanged && pageContent.includes('aria-label="发送')) {
                    return "is_finish";
               };
               return "not";
           })();
        """
        return js_script, "finish"
