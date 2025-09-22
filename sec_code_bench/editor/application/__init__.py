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


import shutil
import subprocess
import time
from abc import abstractmethod
from typing import Any

import psutil
import websocket

from sec_code_bench.editor.abstract import Editor
from sec_code_bench.utils.cdp_utils import CdpOperator
from sec_code_bench.utils.logger_utils import Logger

LOG = Logger.get_logger(__name__)


class IdeEditor(Editor):
    """Abstract base class for IDE editors."""

    def __init__(self, timeout: int = 300) -> None:
        """
        Initialize the IDE Editor instance.

        Args:
            timeout: Maximum time allowed for code generation in seconds (default: 300)
        """
        super().__init__(timeout)
        self.port = 9224
        self._closed = False
        # for sure can start from shell
        binary = self._get_binary_name()
        binary_path = shutil.which(binary)
        if not binary_path:
            raise FileNotFoundError(
                f"Cannot find binary: {binary}, install ide command first."
            )
        # need kill exists first，for sure listen debug port
        self.close()
        # then run a new windows
        args = [
            binary_path,
            "--verbose",
            f"--remote-debugging-port={self.port}",
            f"--remote-allow-origins=*",
        ]
        try:
            self.proc = subprocess.Popen(
                args,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.STDOUT,
            )
        except Exception as e:
            LOG.error(f"Failed to start process: {e}")
        time.sleep(1)
        # and close all windows
        closed_windows = []
        for data in CdpOperator.get_data():
            con = websocket.create_connection(data["webSocketDebuggerUrl"], timeout=10)
            CdpOperator.close_windows(con)
            closed_windows.append(data.get("title", "Unknown"))

        # Verify all windows are closed
        if closed_windows:
            LOG.info(f"Closed {len(closed_windows)} windows: {closed_windows}")
            # Check if any windows are still accessible
            try:
                remaining_views = CdpOperator.get_data()
                if remaining_views:
                    LOG.warning(
                        f"Warning: {len(remaining_views)} windows "
                        "still accessible after closing"
                    )
                    for view in remaining_views:
                        LOG.warning(
                            f"  - {view.get('title', 'Unknown')} (ID: {view.get('id', 'N/A')})"
                        )
                else:
                    LOG.info("All windows successfully closed and verified")
            except Exception as e:
                LOG.info(f"Unable to verify window closure (likely all closed): {e}")

    def __enter__(self) -> "IdeEditor":
        """
        Enter the runtime context.

        Returns:
            IdeEditor instance
        """
        return self

    def __exit__(
        self,
        exc_type: type | None,
        exc_val: Exception | None,
        exc_tb: Any | None,
    ) -> bool | None:
        """
        Exit the runtime context and close the editor.

        Args:
            exc_type: Exception type
            exc_val: Exception value
            exc_tb: Exception traceback

        Returns:
            Whether the exception was handled
        """
        if not self._closed:
            self.close()
        return None

    def __del__(self) -> None:
        """Destructor to ensure the editor is closed."""
        if not self._closed:
            self.close()

    def close(self) -> None:
        """Close the editor process."""
        for proc in psutil.process_iter(["pid", "name", "cmdline"]):
            if (
                proc.info["name"] == "Electron"
                and self.get_editor() in proc.info["cmdline"][0]
            ) and "Electron" in proc.info["cmdline"][0]:
                LOG.info(
                    f"found {self.get_editor()} process: {proc.info['pid']} "
                    f"{proc.info['cmdline']}, try kill ..."
                )
                proc.kill()
        time.sleep(1)

    def open(self, code_dir: str, need_prepare: bool) -> None:
        """
        Open the editor with the specified code directory.

        Args:
            code_dir: Path to the code directory
            need_prepare: Whether preparation steps are needed
        """
        args = [
            self._get_binary_name(),
            code_dir,
        ]
        try:
            self.proc = subprocess.Popen(
                args,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.STDOUT,
            )
        except Exception as e:
            raise RuntimeError(f"Failed to start process: {e}") from e

        if need_prepare:
            self.prepare(code_dir)

    def coding(
        self, code_dir: str, prompt: str, need_prepare: bool = False, debug: bool = False
    ) -> None:
        """
        Generate code based on the given prompt.

        Args:
            code_dir: Running code directory
            prompt: The prompt to guide code generation
            need_prepare: Whether preparation steps are needed (default: False)
            debug: Enable debug mode for application type editors (default: False)
        """
        self.open(code_dir, need_prepare)
        time.sleep(1)
        LOG.info(
            "try connect cdp websocket port...",
        )
        
        main_ws = None
        child_ws = None
        
        try:
            main_ws = CdpOperator.get_page_connect(code_dir)
            if main_ws is None:
                raise Exception("can not get websocket connect")
                
            if not self._call_pages(main_ws):
                raise Exception(
                    f"run failed: wait {self.timeout} second and still "
                    "unable to make the tag selected."
                )

            if self.get_type() != "embed":
                child_ws = CdpOperator.get_child_pages_connect(code_dir, self.get_type())
                self._code(child_ws, prompt)
            else:
                self._code(main_ws, prompt)
                
        except Exception as e:
            if debug:
                LOG.info("Debug mode enabled, saving debug snapshots due to exception...")
                try:
                    debug_ws = child_ws if child_ws else main_ws
                    if debug_ws:
                        self.save_debug(debug_ws)
                except Exception as debug_e:
                    LOG.error(f"Failed to save debug snapshots: {debug_e}")
            raise e
        finally:
            try:
                LOG.info(f"Closing child window connection for {code_dir}...")
                CdpOperator.close_windows_with_verification(main_ws, code_dir)
            except Exception as e:
                LOG.error(f"Error closing child window: {e}")

    @abstractmethod
    def _get_pages_selector(self) -> tuple[str, str]:
        """
        Get the page selector.

        Returns:
            Tuple containing JavaScript script and selector
        """
        pass

    @abstractmethod
    def _get_focus_sign(self) -> tuple[str, str]:
        """
        Get the focus sign.

        Returns:
            Tuple containing JavaScript script and target selector
        """
        pass

    @abstractmethod
    def _get_finish_sign(self) -> tuple[str, str]:
        """
        Get the finish sign.

        Returns:
            Tuple containing JavaScript script and flag
        """
        pass

    def prepare(self, code_dir: str) -> None:
        """
        Prepare the environment for coding.

        Args:
            code_dir: Path to the code directory
        """
        pass

    def _code(self, ws: websocket.WebSocket, prompt: str) -> None:
        """
        Send code to the editor.

        Args:
            ws: WebSocket connection
            prompt: The prompt to guide code generation
        """
        if self._call_focus(ws):
            CdpOperator.send_input_text(ws, prompt)
        else:
            raise Exception(
                "Failed to locate key input box，can not input prompt",
            )

        if self._wait(ws, check_interval=1):
            LOG.info(
                "Detected that the final result page has been generated",
            )
        else:
            LOG.error(
                "Waiting for result page timeout, continue execution",
            )
        return

    def _wait(self, ws: websocket.WebSocket, check_interval: int = 2) -> bool:
        """
        Wait for the coding process to finish.

        Args:
            ws: WebSocket connection
            check_interval: Interval between checks in seconds (default: 2)

        Returns:
            True if the process finished successfully, False otherwise
        """
        ranger = self.timeout / check_interval
        for _i in range(int(ranger)):
            try:
                js_script, flag = self._get_finish_sign()
                out = CdpOperator.evaluate_js(ws, js_script, await_promise=True)
                if out is not None and flag in out:
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
            f"Waiting for final result page timeout, already waiting {self.timeout} s",
        )
        return False

    def _call_focus(self, ws: websocket.WebSocket) -> bool:
        """
        Focus on the input element.

        Args:
            ws: WebSocket connection

        Returns:
            True if successfully focused, False otherwise
        """
        timeout = 10
        retry_interval = 1
        focus_js, target_selector = self._get_focus_sign()
        start_time = time.time()
        while time.time() - start_time < 10:
            try:
                if CdpOperator.evaluate_js(ws, focus_js, await_promise=True):
                    LOG.info(
                        f"Status: Successfully focused on element "
                        f"(used {time.time() - start_time:.2f} s)。",
                    )
                    return True
            except Exception as e:
                LOG.error(e)
                pass
            time.sleep(retry_interval)

        LOG.error(
            f"Execution failed: waiting {timeout} s, "
            f"still unable to focus '{target_selector}'。",
        )
        return False

    def _call_pages(self, ws: websocket.WebSocket) -> bool:
        """
        Call pages and ensure the page is selected.

        Args:
            ws: WebSocket connection

        Returns:
            True if successful, False otherwise
        """
        timeout = 30
        retry_interval = 1

        CdpOperator.send_command(ws, "Runtime.enable")
        js_script, selector = self._get_pages_selector()

        if js_script != "" and selector != "":
            LOG.info(
                "Step: Ensuring that the page is selected "
                f"(timeout: {timeout} seconds)..."
            )
            start_time = time.time()
            flag = True
            while time.time() - start_time < timeout and flag:
                try:
                    if CdpOperator.evaluate_js(ws, js_script, await_promise=True):
                        LOG.info("Status: The element is already selected.")
                        flag = False
                except Exception as e:
                    LOG.error(f"evaluate_js error: {str(e)}")
                    pass
                time.sleep(retry_interval)
            if flag:
                raise Exception("wait for pages connect time out")

        LOG.info("Waiting for the initialization of the new panel...")
        time.sleep(1)
        return True

    @staticmethod
    def save_debug(ws: websocket.WebSocket):
        try:
            html_content = CdpOperator.evaluate_js(
                ws, "document.documentElement.outerHTML"
            )
            if html_content:
                with open("index.html", "w") as f:
                    f.write(html_content)
                LOG.info("[✓] index page snapshot successfully saved")

            else:
                LOG.error("[✗] unable to retrieve the HTML content of the page。")
        except Exception as e:
            LOG.error(f"[✗] error occurred while saving DOM snapshot: {e}")
        try:
            js_script = """
               (() => {
                   const iframe = document.querySelector('#active-frame');
                   if (iframe && iframe.contentDocument) {
                       return iframe.contentDocument.documentElement.outerHTML;
                   };
                   const iframe2 = document.querySelector('iframe.webview.ready');
                   if (iframe2 && iframe2.contentDocument) {
                       return iframe2.contentDocument.documentElement.outerHTML;
                   }
                   return null;
               })();
               """
            html_content = CdpOperator.evaluate_js(ws, js_script)

            if html_content:
                with open("iframe.html", "w") as f:
                    f.write(html_content)
                LOG.info("[✓] iframe page snapshot successfully saved")
            else:
                LOG.error(
                    "[✗] Unable to retrieve the HTML content of iframe '#active frame'. "
                    "It may be cross domain or not loaded"
                )
        except Exception as e:
            LOG.error(f"[✗] Error occurred while saving iframe DOM snapshot: {e}")
