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


class CursorEditor(IdeEditor):
    """Cursor IDE editor implementation."""

    def _get_binary_name(self) -> str:
        """
        Get the name of the Cursor binary.

        Returns:
            Binary name as string
        """
        return "cursor"

    def get_editor(self) -> str:
        """
        Get the editor name.

        Returns:
            Editor name as string
        """
        return "Cursor"

    def get_type(self) -> str:
        """
        Get the editor type.

        Returns:
            Editor type as string
        """
        return "embed"

    def _get_pages_selector(self) -> tuple[str, str]:
        """
        Get the pages selector for Cursor IDE.

        Returns:
            Tuple of (JS script, selector)
        """
        selector = 'div[autocapitalize="off"]'
        js_script = f"""
                   (() => {{
                       const el = document.querySelector('{selector}');
                       if (!el) return false;

                       el.focus();

                       const selection = window.getSelection();
                       const range = document.createRange();

                       range.selectNodeContents(el);
                       range.collapse(true);

                       selection.removeAllRanges();
                       selection.addRange(range);

                       return true;
                   }})();
                   """
        return js_script, selector

    def _get_focus_sign(self) -> tuple[str, str]:
        """
        Get the focus sign for Cursor IDE.

        Returns:
            Tuple of (JS script, target selector)
        """
        click_position_window_js = """
                   (() => {
                       const positionWindow = document.querySelector('.position-window') ||
                                             document.querySelector('.right-panel') ||
                                             document.querySelector('.sidebar-right');

                       if (positionWindow) {
                           positionWindow.click();
                           return true;
                       } else {
                           const rightPanels = Array.from(document.querySelectorAll('div, section, aside'))
                               .filter(el => {
                                   const rect = el.getBoundingClientRect();
                                   return rect.right > window.innerWidth * 0.7 &&
                                          rect.height > window.innerHeight * 0.3;
                               });

                           if (rightPanels.length > 0) {
                               rightPanels[0].click();
                               return true;
                           }
                           return false;
                       }
                   })();
               """
        return click_position_window_js, "auto"

    def _get_finish_sign(self) -> tuple[str, str]:
        """
        Get the finish sign for Cursor IDE.

        Returns:
            Tuple of (JS script, flag)
        """
        result_check_js = """
                           (() => {
                               const pageContent = document.documentElement.outerHTML;
                               return pageContent;
                           })();
                           """
        return result_check_js, "Review Changes"
