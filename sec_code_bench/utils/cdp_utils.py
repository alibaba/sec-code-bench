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

import json
import time
from pathlib import Path
from typing import Any

import requests
import websocket

from sec_code_bench.utils.logger_utils import Logger

_cdp_message_id = 0

LOG = Logger.get_logger(__name__)


class CdpOperator:
    """Chrome DevTools Protocol (CDP) operator for browser automation."""

    @staticmethod
    def get_data() -> list[dict[str, Any]]:
        """
        Get data from the Chrome DevTools Protocol endpoint.

        Returns:
            List of target information dictionaries

        Raises:
            Exception: If unable to access the connection
        """
        try:
            url = "http://localhost:9224/json"
            response = requests.get(url)
            response.raise_for_status()

            return response.json()
        except requests.exceptions.RequestException as e:
            raise Exception(f"can not access connect: {e}") from e

    @staticmethod
    def get_page_connect(code_dir: str) -> websocket.WebSocket:
        """
        Get page connection for the specified code directory.

        Args:
            code_dir: Code directory path

        Returns:
            WebSocket connection

        Raises:
            Exception: If unable to connect to debug port after maximum retries
        """
        max_retry = 5
        retry = 0
        interval = 1
        while retry < max_retry:
            try:
                views = CdpOperator.get_data()
                for view in views:
                    if Path(code_dir).name in view["title"]:
                        try:
                            con = websocket.create_connection(
                                view["webSocketDebuggerUrl"], timeout=10
                            )
                            return con
                        except Exception as e:
                            LOG.error("websocket connect error", str(e))
            except Exception as e:
                LOG.warning(f"Error connecting to debug port: {e}, retrying {retry}...")
            retry += 1
            time.sleep(interval)
        raise Exception(
            "Unable to connect to debug port, maximum retry attempts "
            "have been reached, exit process ..."
        )

    @staticmethod
    def get_child_pages_connect(code_dir: str, extends_id: str) -> websocket.WebSocket:
        """
        Get child pages connection for the specified code directory and extension ID.

        Args:
            code_dir: Code directory path
            extends_id: Extension ID

        Returns:
            WebSocket connection

        Raises:
            Exception: If unable to connect to debug port after maximum retries
        """
        max_retry = 5
        retry = 0
        interval = 1
        parent_id = ""
        while retry < max_retry:
            try:
                views = CdpOperator.get_data()
                for view in views:
                    if Path(code_dir).name in view["title"]:
                        parent_id = view["id"]
                if parent_id != "":
                    for view in views:
                        if (
                            "parentId" in view
                            and view["parentId"] == parent_id
                            and extends_id in view["title"]
                        ):
                            try:
                                con = websocket.create_connection(
                                    view["webSocketDebuggerUrl"], timeout=10
                                )
                                return con
                            except Exception as e:
                                LOG.error("websocket connect error", str(e))
            except Exception as e:
                LOG.warning(f"Error connecting to debug port: {e}, retrying {retry}...")
            retry += 1
            time.sleep(interval)
        raise Exception(
            "Unable to connect to debug port, maximum retry attempts "
            "have been reached, exit process ..."
        )

    @staticmethod
    def send_command(
        ws: websocket.WebSocket, method: str, params: dict[str, Any] | None = None
    ) -> dict[str, Any]:
        """
        Send a command to the Chrome DevTools Protocol.

        Args:
            ws: WebSocket connection
            method: CDP method name
            params: Parameters for the method (default: None)

        Returns:
            Command result as dictionary

        Raises:
            Exception: If CDP command returns an error or times out
        """
        global _cdp_message_id
        if params is None:
            params = {}

        _cdp_message_id += 1
        command_id = _cdp_message_id
        message = {"id": command_id, "method": method, "params": params}
        ws.send(json.dumps(message))
        while True:
            try:
                response_str = ws.recv()
                response = json.loads(response_str)
                if response.get("id") == command_id:
                    if "error" in response:
                        raise Exception(
                            f"CDP command '{method}' return error: "
                            f"{response['error']['message']}"
                        )
                    return response.get("result", {})
            except websocket.WebSocketTimeoutException as e:
                raise Exception(f"wait cdp command '{method}' timeout.") from e

    @classmethod
    def send_input_text(cls, ws: websocket.WebSocket, text: str) -> None:
        """
        Send input text to the browser.

        Args:
            ws: WebSocket connection
            text: Text to send
        """
        cls.send_command(ws, "Input.insertText", {"text": text})
        time.sleep(0.3)
        cls.press_enter(ws)

    @classmethod
    def evaluate_js(
        cls, ws: websocket.WebSocket, js_expression: str, await_promise: bool = False
    ) -> Any:
        """
        Evaluate JavaScript expression in the browser context.

        Args:
            ws: WebSocket connection
            js_expression: JavaScript expression to evaluate
            await_promise: Whether to await promise results (default: False)

        Returns:
            Evaluation result
        """
        result = cls.send_command(
            ws,
            "Runtime.evaluate",
            {
                "expression": js_expression,
                "returnByValue": True,
                "awaitPromise": await_promise,
            },
        )
        return result.get("result", {}).get("value")

    @classmethod
    def press_enter(cls, ws: websocket.WebSocket) -> None:
        """
        Simulate pressing the Enter key.

        Args:
            ws: WebSocket connection
        """
        cls.send_command(
            ws,
            "Input.dispatchKeyEvent",
            {
                "type": "keyDown",
                "key": "Enter",
                "code": "Enter",
                "windowsVirtualKeyCode": 13,
            },
        )
        cls.send_command(
            ws,
            "Input.dispatchKeyEvent",
            {
                "type": "keyUp",
                "key": "Enter",
                "code": "Enter",
                "windowsVirtualKeyCode": 13,
            },
        )

    @classmethod
    def close_windows(cls, ws: websocket.WebSocket) -> None:
        """
        Close the browser window.

        Args:
            ws: WebSocket connection
        """
        cls.send_command(ws, "Runtime.evaluate", {"expression": "window.close();"})

    @staticmethod
    def check_connection_status(
        code_dir: str, max_checks: int = 3, check_interval: float = 1.0
    ) -> bool:
        """
        Check if CDP connection is properly closed by attempting to get data multiple times.

        Args:
            code_dir: Code directory path to check for
            max_checks: Maximum number of checks to perform (default: 3)
            check_interval: Interval between checks in seconds (default: 1.0)

        Returns:
            bool: True if connection is properly closed, False if still accessible
        """
        LOG.info(f"Checking CDP connection status for {code_dir}...")

        for attempt in range(1, max_checks + 1):
            try:
                views = CdpOperator.get_data()
                matching_views = [
                    view for view in views if Path(code_dir).name in view.get("title", "")
                ]

                if matching_views:
                    LOG.warning(
                        f"Check {attempt}/{max_checks}: Found {len(matching_views)} matching views still accessible"
                    )
                    if attempt < max_checks:
                        LOG.info(f"Waiting {check_interval} seconds before next check...")
                        time.sleep(check_interval)
                    else:
                        # Final check failed - connection still accessible
                        LOG.error(
                            f"CDP connection check failed after {max_checks} attempts!"
                        )
                        LOG.error(f"Still found {len(matching_views)} matching views:")
                        for i, view in enumerate(matching_views, 1):
                            LOG.error(
                                f"  {i}. ID: {view.get('id', 'N/A')}, Title: {view.get('title', 'N/A')}"
                            )
                        return False
                else:
                    LOG.info(
                        f"Check {attempt}/{max_checks}: No matching views found - connection properly closed"
                    )
                    return True

            except Exception as e:
                LOG.info(
                    f"Check {attempt}/{max_checks}: Unable to access CDP endpoint - connection likely closed: {e}"
                )
                return True

        return True

    @classmethod
    def close_windows_with_verification(
        cls, ws: websocket.WebSocket, code_dir: str
    ) -> None:
        """
        Close the browser window and verify the connection is properly closed.

        Args:
            ws: WebSocket connection
            code_dir: Code directory path for verification
        """
        LOG.info(f"Closing browser window for {code_dir}...")

        try:
            cls.close_windows(ws)
        except Exception as e:
            LOG.error(f"Error closing browser window: {e}")

        # Wait a moment for the window to close
        time.sleep(1)
        
        # Verify the connection is properly closed
        try:
            if not cls.check_connection_status(code_dir):
                LOG.error(
                    f"WARNING: CDP connection for {code_dir} may not have been properly closed!"
                )
                LOG.error("This could indicate a resource leak or improper cleanup.")
            else:
                LOG.info(
                    f"CDP connection for {code_dir} successfully closed and verified."
                )
        except Exception as e:
            LOG.error(f"Error verifying connection closure: {e}")
