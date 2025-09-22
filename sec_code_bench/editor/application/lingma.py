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


class LingMaEditor(IdeEditor):
    """CodeBuddy IDE editor implementation."""

    def _get_binary_name(self) -> str:
        """
        Get the name of the Qoder binary.

        Returns:
            Binary name as string
        """
        return "Lingma"

    def get_editor(self) -> str:
        """
        Get the editor name.

        Returns:
            Editor name as string
        """
        return "Lingma.app"

    def get_type(self) -> str:
        """
        Get the editor type.

        Returns:
            Editor type as string
        """
        return "aicode.chatView"

    def _get_pages_selector(self) -> tuple[str, str]:
        """
        Get the pages selector for Lingma IDE.

        Returns:
            Tuple of (JS script, selector)
        """
        return (
            """
         (() => {
           const iframe = document.querySelector('iframe.webview.ready');
           return iframe !== null;
       })();""",
            "iframe",
        )

    def _get_focus_sign(self) -> tuple[str, str]:
        """
        Get the focus sign for Lingma IDE.

        Returns:
            Tuple of (JS script, target selector)
        """
        target_selector = "#chat-input-row"
        focus_js = """
               (() => {
                   const iframe = document.querySelector('#active-frame');
                   if (!iframe) return false;
                   const iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
                   if (!iframeDoc) return false;
                   const el = iframeDoc.querySelector('textarea.ant-input.chat-input');
                   return el;
                   if (el) {
                       iframe.contentWindow.focus();
                       el.focus();
                       return iframeDoc.activeElement === el;
                   }
                   return false;
               })();
               """
        return focus_js, target_selector

    def _get_finish_sign(self) -> tuple[str, str]:
        """
        Get the finish sign for VS Code Lingma IDE.

        Returns:
            Tuple of (JS script, flag)
        """
        js_script = """
                 (() => {{
                     const iframe = document.querySelector('#active-frame');
                     if (iframe && iframe.contentDocument) {{
                         return iframe.contentDocument.documentElement.outerHTML;
                     }}
                     return null;
                 }})();
                 """
        return js_script, "重新生成"
