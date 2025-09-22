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


class VscodeBuddyEditor(IdeEditor):
    """VS Code Buddy IDE editor implementation."""

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
        return "Tencent-Cloud.coding-copilot"

    def _get_pages_selector(self) -> tuple[str, str]:
        """
        Get the pages selector for VS Code Buddy IDE.

        Returns:
            Tuple of (JS script, selector)
        """
        selector = 'a[aria-label="CodeBuddy"]'
        js_script = f"""
        (() => {{
            const el_a = document.querySelector('{selector}');
            if (!el_a) return false;
            const parent_li = el_a.closest('li');
            if (!parent_li) return false;
            if (parent_li.classList.contains('checked')) return true;
            el_a.click();
            return false;
        }})();
        """
        return js_script, selector

    def _get_focus_sign(self) -> tuple[str, str]:
        """
        Get the focus sign for VS Code Buddy IDE.

        Returns:
            Tuple of (JS script, target selector)
        """
        iframe_selector = "#active-frame"
        advanced_focus_js = f"""
           (() => {{
               try {{
                   const iframe = document.querySelector('{iframe_selector}');
                   if (!iframe) return "no iframe";

                   const doc = iframe.contentDocument || iframe.contentWindow.document;
                   if (!doc) return "can not access iframe";

                   let targetElement = null;
                   const possibleElements = [
                       ...Array.from(doc.querySelectorAll('textarea')),
                       ...Array.from(doc.querySelectorAll('[contenteditable="true"]')),
                       ...Array.from(doc.querySelectorAll('input[type="text"]'))
                   ];

                   if (possibleElements.length > 0) {{
                       targetElement = possibleElements[0];

                       iframe.contentWindow.focus();
                       targetElement.focus();

                       const clickEvent = new MouseEvent('mousedown', {{
                           bubbles: true,
                           cancelable: true,
                           view: iframe.contentWindow
                       }});
                       targetElement.dispatchEvent(clickEvent);

                       const upEvent = new MouseEvent('mouseup', {{
                           bubbles: true,
                           cancelable: true,
                           view: iframe.contentWindow
                       }});
                       targetElement.dispatchEvent(upEvent);

                       targetElement.click();
                       targetElement.focus();

                       return true;
                   }}

                   return false;
               }} catch (e) {{
                   return false;
               }}
           }})();
           """
        return advanced_focus_js, iframe_selector

    def _get_finish_sign(self) -> tuple[str, str]:
        """
        Get the finish sign for VS Code Buddy IDE.

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
        return js_script, '<div class="feedback__icon"><span class="copy" title="复制">'
