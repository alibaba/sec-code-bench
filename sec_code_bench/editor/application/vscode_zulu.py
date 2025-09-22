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

import time

import websocket

from sec_code_bench.editor.application import IdeEditor
from sec_code_bench.utils.cdp_utils import CdpOperator
from sec_code_bench.utils.logger_utils import Logger

LOG = Logger.get_logger(__name__)


class VscodeZuluEditor(IdeEditor):
    """VS Code Zulu IDE editor implementation."""

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
        return "BaiduComate.comate"

    def _get_pages_selector(self) -> tuple[str, str]:
        """
        Get the pages selector for VS Code Zulu IDE.

        Returns:
            Tuple of (JS script, selector)
        """
        selector = 'a[aria-label="文心快码 ( Baidu Comate )"]'
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
        Get the focus sign for VS Code Zulu IDE.

        Returns:
            Tuple of (JS script, target selector)
        """
        iframe_selector = "#active-frame"
        advanced_focus_js = f"""
           (() => {{
               try {{
                   const iframe = document.querySelector('{iframe_selector}');
                   if (!iframe) return "can not found iframe";

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
        Get the finish sign for VS Code Zulu IDE.

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
        return (
            js_script,
            '停止生成',
        )

    def _wait(self, ws: websocket.WebSocket, check_interval: int = 2) -> bool:
        """
        Wait for the code generation process to finish.

        Args:
            ws: WebSocket connection
            check_interval: Interval between checks in seconds (default: 2)

        Returns:
            True if generation finished, False otherwise
        """
        LOG.info("Waiting for the final result to be generated...")
        ranger = self.timeout / check_interval
        is_start = False
        for _i in range(int(ranger)):
            try:
                js_script, flag = self._get_finish_sign()
                out = CdpOperator.evaluate_js(ws, js_script, await_promise=True)
                if not is_start:
                    if out is not None and flag in out:
                        LOG.info( "Detected that the result is generating.... ")
                        is_start = True
                else:
                    if out is not None and flag not in out:
                        LOG.info(
                            "Detected that the result has been generated "
                            "and the page contains the word 'end'"
                        )
                        return True
            except Exception as e:
                LOG.error(e)
                pass
            time.sleep(check_interval)

        LOG.error(
            f"Waiting for final result page timeout, waiting for {self.timeout} seconds"
        )
        return False
