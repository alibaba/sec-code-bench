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


class QoderEditor(IdeEditor):
    """Qoder IDE editor implementation."""

    def _get_binary_name(self) -> str:
        """
        Get the name of the Qoder binary.

        Returns:
            Binary name as string
        """
        return "qoder"

    def get_editor(self) -> str:
        """
        Get the editor name.

        Returns:
            Editor name as string
        """
        return "Qoder"

    def get_type(self) -> str:
        """
        Get the editor type.

        Returns:
            Editor type as string
        """
        return "embed"

    def _get_pages_selector(self) -> tuple[str, str]:
        """
        Get the pages selector for Qoder IDE.

        Returns:
            Tuple of (JS script, selector)
        """
        return (
            """
         (() => {
               const chatContainer = document.querySelector('.chat-mixed-input-container');
               const inputElement = document.querySelector('.chat-input-contenteditable');
               const agentContainer = document.querySelector('.agentchat-container');
               
               return !!(chatContainer && inputElement && agentContainer);
       })();""",
            ".chat-mixed-input-container",
        )

    def _get_focus_sign(self) -> tuple[str, str]:
        """
        Get the focus sign for Qoder IDE.
        Instead of just focusing, we directly prepare the input element for text insertion.

        Returns:
            Tuple of (JS script, target selector)
        """
        target_selector = ".chat-input-contenteditable"
        focus_js = """
                       (() => {
                           // Find the input element
                           const selectors = [
                               '.chat-input-contenteditable[contenteditable="true"]',
                               '.chat-input-contenteditable',
                               '.agentchat-inner-container .chat-input-contenteditable',
                               '[data-placeholder="自主规划及编程..."]',
                               '[contenteditable="true"]'
                           ];
                           
                           let inputElement = null;
                           for (const selector of selectors) {
                               inputElement = document.querySelector(selector);
                               if (inputElement && inputElement.contentEditable === 'true') {
                                   break;
                               }
                           }
                           
                           if (!inputElement) {
                               return false;
                           }
                           
                           // Store the input element globally for direct text insertion
                           window._qoderInputElement = inputElement;
                           
                           // Clear content
                           inputElement.innerHTML = '';
                           inputElement.textContent = '';
                           
                           // Try to focus (but don't rely on it)
                           inputElement.focus();
                           inputElement.click();
                           
                           // Always return true since we found the element
                           return true;
                       })();
                       """
        return focus_js, target_selector

    def _get_finish_sign(self) -> tuple[str, str]:
        """
        Get the finish sign for Qoder IDE.

        Returns:
            Tuple of (JS script, flag)
        """
        js_script = """
                        (() => {{
                            const hasSpanChanged = Array.from(document.querySelectorAll('span')).some(span => {
                            const text = span.textContent.trim();
                                return text.includes('变更文件');
                            });
                           const pageContent = document.documentElement.outerHTML;
                           if(hasSpanChanged && pageContent.includes('aria-label="Send message"')) {
                                return "is_finish";
                           };
                           return "not";
                        }})();
                        """
        return js_script, "is_finish"