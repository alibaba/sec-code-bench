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

import os
import signal
import subprocess
import sys
import threading
import time
import websocket

from sec_code_bench.editor.application import IdeEditor
from sec_code_bench.utils.cdp_utils import CdpOperator
from sec_code_bench.utils.logger_utils import Logger

LOG = Logger.get_logger(__name__)

_GLOBAL_TERMINATION_IN_PROGRESS = False
_GLOBAL_TERMINATION_LOCK = threading.Lock()
_GLOBAL_TERMINATION_EVENT = threading.Event()


class TokenLimitExceededException(Exception):
    """Exception raised when token limit is exceeded."""

    def __init__(self, message: str = "Token limit exceeded", terminate_all: bool = True):
        super().__init__(message)
        self.message = message

        if terminate_all:
            self._terminate_all_threads_and_exit()

    def _terminate_all_threads_and_exit(self):
        """
        stop all threads and exit.
        """
        global _GLOBAL_TERMINATION_IN_PROGRESS, _GLOBAL_TERMINATION_LOCK, _GLOBAL_TERMINATION_EVENT

        is_termination_thread = False
        with _GLOBAL_TERMINATION_LOCK:
            if _GLOBAL_TERMINATION_IN_PROGRESS:
                current_thread_name = threading.current_thread().name
                LOG.error(
                    f"Global termination already in progress by another thread. Current thread {current_thread_name} waiting for termination to complete...")
            else:
                _GLOBAL_TERMINATION_IN_PROGRESS = True
                is_termination_thread = True
                current_thread_name = threading.current_thread().name
                LOG.error("=" * 80)
                LOG.error(
                    f"CRITICAL: Token limit exceeded! Thread '{current_thread_name}' initiating global shutdown...")
                LOG.error(f"Error message: {self.message}")
                LOG.error("=" * 80)

        if not is_termination_thread:
            LOG.error(f"Thread {threading.current_thread().name} waiting for global termination to complete...")
            if _GLOBAL_TERMINATION_EVENT.wait(timeout=30):
                LOG.error(f"Thread {threading.current_thread().name} detected termination completed, exiting...")
            else:
                LOG.error(f"Thread {threading.current_thread().name} timeout waiting for termination, force exiting...")
            return

        try:
            current_pid = os.getpid()
            LOG.error(f"Current main process PID: {current_pid}")

            main_thread = threading.main_thread()
            LOG.error(f"Main thread: {main_thread.name}")

            active_threads = threading.enumerate()
            LOG.error(f"Found {len(active_threads)} active threads:")

            for thread in active_threads:
                LOG.error(f"  - Thread: {thread.name}, Daemon: {thread.daemon}, Alive: {thread.is_alive()}")

            LOG.error("Closing all VSCode windows before termination...")
            self._close_all_vscode_windows()

            self._set_global_exit_flag()

            LOG.error("Attempting to stop all threads...")
            current_thread = threading.current_thread()
            non_daemon_threads = [t for t in active_threads if
                                  t != main_thread and not t.daemon and t.is_alive() and t != current_thread]

            for thread in non_daemon_threads:
                try:
                    LOG.error(f"Signaling thread {thread.name} to stop...")
                    thread.join(timeout=1.0)  # 只等待1秒
                    if thread.is_alive():
                        LOG.error(f"Thread {thread.name} still running - will be terminated with process")
                    else:
                        LOG.error(f"Thread {thread.name} finished")
                except Exception as e:
                    LOG.error(f"Error with thread {thread.name}: {e}")

            if current_thread != main_thread:
                LOG.error(f"Current thread {current_thread.name} is not main thread - terminating from worker thread")

            LOG.error("Sending SIGTERM to current process...")
            try:
                os.kill(current_pid, signal.SIGTERM)
                time.sleep(0.5)
            except Exception as e:
                LOG.error(f"Error sending SIGTERM: {e}")

            LOG.error("Cleaning up resources...")
            self._cleanup_resources()

            LOG.error("Notifying waiting threads that termination is completing...")
            _GLOBAL_TERMINATION_EVENT.set()

            LOG.error("Force exiting main process...")
            LOG.error("=" * 80)

            sys.exit(1)

        except Exception as e:
            LOG.error(f"Error during global shutdown: {e}")
            # 通知等待的线程终止流程完成（即使有错误）
            LOG.error("Notifying waiting threads that termination is completing (with error)...")
            _GLOBAL_TERMINATION_EVENT.set()
            # 最后的手段：直接退出
            LOG.error("Using sys.exit as last resort...")
            sys.exit(1)

    def _set_global_exit_flag(self):
        """
        设置全局退出标志，通知其他线程停止工作
        """
        try:
            # 尝试设置一些可能存在的全局标志
            import threading

            # 创建一个全局事件来通知所有线程
            if not hasattr(self, '_global_exit_event'):
                self._global_exit_event = threading.Event()
            self._global_exit_event.set()

            # 尝试查找并停止线程池执行器
            self._shutdown_thread_pools()

        except Exception as e:
            LOG.error(f"Error setting global exit flag: {e}")

    def _shutdown_thread_pools(self):
        """
        尝试关闭可能存在的线程池执行器
        """
        try:
            import gc
            from concurrent.futures import ThreadPoolExecutor

            # 查找所有 ThreadPoolExecutor 实例
            shutdown_executors = set()  # 防止重复关闭同一个执行器
            for obj in gc.get_objects():
                if isinstance(obj, ThreadPoolExecutor):
                    executor_id = id(obj)
                    if executor_id not in shutdown_executors:
                        try:
                            LOG.error(f"Shutting down ThreadPoolExecutor: {obj} (ID: {executor_id})")
                            obj.shutdown(wait=False)  # 立即关闭，不等待
                            shutdown_executors.add(executor_id)
                        except Exception as e:
                            LOG.error(f"Error shutting down ThreadPoolExecutor {executor_id}: {e}")

        except Exception as e:
            LOG.error(f"Error during thread pool shutdown: {e}")

    def _close_all_vscode_windows(self):
        """
        关闭所有 VSCode 窗口，防止在终止时留下残留窗口
        """
        try:
            LOG.error("Closing all VSCode windows using CdpOperator...")

            # 使用 CdpOperator 获取所有页面数据
            try:
                pages = CdpOperator.get_data()
                LOG.error(f"Found {len(pages)} CDP pages/windows")

                # 关闭每个页面/窗口
                closed_count = 0
                for page in pages:
                    try:
                        page_id = page.get('id', 'Unknown')
                        page_title = page.get('title', 'Unknown')
                        page_type = page.get('type', 'Unknown')
                        ws_url = page.get('webSocketDebuggerUrl')

                        LOG.error(f"Attempting to close CDP page: {page_title} (Type: {page_type}, ID: {page_id})")

                        # 尝试通过 WebSocket 关闭页面
                        if ws_url:
                            try:
                                import websocket
                                ws = websocket.create_connection(ws_url, timeout=3)

                                # 使用 CdpOperator 的方法关闭窗口
                                CdpOperator.close_windows(ws)
                                ws.close()

                                LOG.error(f"Successfully closed page: {page_title}")
                                closed_count += 1

                            except Exception as e:
                                LOG.error(f"Error closing page {page_title} via WebSocket: {e}")
                        else:
                            LOG.error(f"No WebSocket URL for page: {page_title}")

                    except Exception as e:
                        LOG.error(f"Error processing page {page.get('title', 'Unknown')}: {e}")

                LOG.error(f"Successfully closed {closed_count} out of {len(pages)} pages")

            except Exception as e:
                LOG.error(f"Error getting CDP pages data: {e}")

            # 额外清理：强制终止 VSCode 进程
            try:
                LOG.error("Attempting to terminate VSCode processes...")
                import psutil
                import os

                current_pid = os.getpid()
                terminated_count = 0

                for proc in psutil.process_iter(['pid', 'name', 'cmdline']):
                    try:
                        proc_info = proc.info
                        proc_name = proc_info['name'].lower() if proc_info['name'] else ''
                        proc_cmdline = ' '.join(proc_info['cmdline']) if proc_info['cmdline'] else ''

                        # 查找 VSCode 相关进程（但排除当前进程）
                        if proc_info['pid'] != current_pid and (
                                'code' in proc_name or
                                'vscode' in proc_name or
                                'electron' in proc_name or
                                '--remote-debugging-port=9224' in proc_cmdline
                        ):
                            LOG.error(f"Found VSCode-related process: PID={proc_info['pid']}, Name={proc_name}")

                            try:
                                proc.terminate()  # 优雅终止
                                try:
                                    proc.wait(timeout=2)  # 等待2秒
                                    LOG.error(f"Successfully terminated process PID={proc_info['pid']}")
                                    terminated_count += 1
                                except psutil.TimeoutExpired:
                                    # 如果优雅终止失败，强制杀死
                                    proc.kill()
                                    LOG.error(f"Force killed process PID={proc_info['pid']}")
                                    terminated_count += 1
                            except psutil.NoSuchProcess:
                                LOG.error(f"Process PID={proc_info['pid']} already terminated")
                            except psutil.AccessDenied:
                                LOG.error(f"Access denied when terminating process PID={proc_info['pid']}")
                            except Exception as e:
                                LOG.error(f"Error terminating process PID={proc_info['pid']}: {e}")

                    except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess):
                        continue
                    except Exception as e:
                        LOG.error(f"Error processing process: {e}")

                LOG.error(f"Terminated {terminated_count} VSCode-related processes")

            except ImportError:
                LOG.error("psutil not available, skipping process termination")
            except Exception as e:
                LOG.error(f"Error during process termination: {e}")

        except Exception as e:
            LOG.error(f"Error during VSCode window cleanup: {e}")

    def _cleanup_resources(self):
        """
        清理可能的资源
        """
        try:
            # 尝试关闭所有打开的文件描述符
            import resource
            max_fd = resource.getrlimit(resource.RLIMIT_NOFILE)[0]

            # 只关闭非标准的文件描述符
            for fd in range(3, min(max_fd, 1024)):  # 跳过 stdin, stdout, stderr
                try:
                    os.close(fd)
                except OSError:
                    pass  # 文件描述符可能已经关闭

        except Exception as e:
            LOG.error(f"Error during resource cleanup: {e}")


class VscodeLingmaEditor(IdeEditor):
    """VS Code Lingma IDE editor implementation."""

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
        return "Alibaba-Cloud.tongyi-lingma"

    def _get_pages_selector(self) -> tuple[str, str]:
        """
        Get the pages selector for VS Code Lingma IDE.

        Returns:
            Tuple of (JS script, selector)
        """
        selector = 'a[aria-label="通义灵码"]'
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
        Get the focus sign for VS Code Lingma IDE.

        Returns:
            Tuple of (JS script, target selector)
        """
        iframe_selector = "#active-frame"
        target_selector = "textarea.chat-input"
        focus_js = f"""
               (() => {{
                   const iframe = document.querySelector('{iframe_selector}');
                   if (!iframe) return false;
                   const iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
                   if (!iframeDoc) return false;
                   const el = iframeDoc.querySelector('{target_selector}');
                   if (el) {{
                       iframe.contentWindow.focus();
                       el.focus();
                       return iframeDoc.activeElement === el;
                   }}
                   return false;
               }})();
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

    def _click_retry_button(self, ws: websocket.WebSocket) -> bool:
        """
        Click the retry button through CDP.
        
        Args:
            ws: WebSocket connection
            
        Returns:
            True if retry button was clicked successfully, False otherwise
        """
        try:
            # JavaScript to find and click the retry button
            click_retry_js = """
            (() => {
                const iframe = document.querySelector('#active-frame');
                if (!iframe || !iframe.contentDocument) return false;
                
                const iframeDoc = iframe.contentDocument;
                
                // Look for the specific retry span element
                const retrySpan = iframeDoc.querySelector('span.ai-button');
                if (retrySpan && retrySpan.textContent && retrySpan.textContent.includes('重试') && 
                    retrySpan.offsetParent !== null) {
                    retrySpan.click();
                    return true;
                }
                
                // Fallback: Look for any span with ai-button class containing retry text
                const aiButtonSpans = Array.from(iframeDoc.querySelectorAll('span.ai-button'));
                for (const span of aiButtonSpans) {
                    if (span.textContent && span.textContent.includes('重试') && 
                        span.offsetParent !== null) {
                        span.click();
                        return true;
                    }
                }
                
                // Additional fallback: Look for any clickable element containing retry text
                const allClickableElements = Array.from(iframeDoc.querySelectorAll('button, span, div, a'));
                for (const element of allClickableElements) {
                    if (element.textContent && 
                        (element.textContent.includes('重试') || 
                         element.textContent.toLowerCase().includes('retry')) &&
                        element.offsetParent !== null) {
                        element.click();
                        return true;
                    }
                }
                
                return false;
            })();
            """

            result = CdpOperator.evaluate_js(ws, click_retry_js, await_promise=True)
            if result:
                LOG.info("Successfully clicked retry button")
                return True
            else:
                LOG.warning("Could not find or click retry button")
                return False

        except Exception as e:
            LOG.error(f"Error clicking retry button: {e}")
            return False

    def _check_for_token_limit(self, html_content: str) -> bool:
        """
        Check if the HTML content contains token limit indicators.
        
        Args:
            html_content: The HTML content to check
            
        Returns:
            True if token limit is detected, False otherwise
        """
        if not html_content:
            return False

        # Common token limit indicators in Chinese and English
        token_limit_indicators = "<p>看起来我们今天已经有了很多的对话，"

        html_lower = html_content.lower()
        if token_limit_indicators.lower() in html_lower:
            LOG.error(f"Token limit detected with indicator: {token_limit_indicators}")
            return True

        return False

    def _check_for_retry_button(self, html_content: str) -> bool:
        """
        Check if the HTML content contains retry button indicators.
        
        Args:
            html_content: The HTML content to check
            
        Returns:
            True if retry button is detected, False otherwise
        """
        if not html_content:
            return False

        # Retry button indicators
        retry_indicators = [
            '>重试</span>'
        ]

        html_lower = html_content.lower()
        for indicator in retry_indicators:
            if indicator.lower() in html_lower:
                LOG.info(f"Retry button detected with indicator: {indicator}")
                return True

        return False

    def _wait(
            self, ws: websocket.WebSocket, check_interval: int = 2
    ) -> bool:
        """
        Enhanced wait method with token limit detection and retry button handling.
        
        Args:
            ws: WebSocket connection
            check_interval: Interval between checks in seconds (default: 2)
            
        Returns:
            True if the process finished successfully, False otherwise
            
        Raises:
            TokenLimitExceededException: When token limit is detected
        """
        ranger = self.timeout / check_interval
        for _i in range(int(ranger)):
            try:
                js_script, flag = self._get_finish_sign()
                out = CdpOperator.evaluate_js(ws, js_script, await_promise=True)

                if out is not None:
                    token_limit_indicators = "<p>看起来我们今天已经有了很多的对话，"
                    # Check for token limit first
                    if token_limit_indicators.lower() in out:
                        error_msg = "Token limit exceeded detected in vscode-lingma. Terminating all threads and main process."
                        LOG.error(error_msg)
                        # 这将立即停止所有线程并退出主进程
                        raise TokenLimitExceededException(error_msg, terminate_all=True)

                    # Check for retry button and auto-click if found
                    if self._check_for_retry_button(out):
                        LOG.info("Retry button detected, attempting to click...")
                        if self._click_retry_button(ws):
                            LOG.info("Retry button clicked, waiting for response...")
                        else:
                            LOG.warning("Failed to click retry button, continuing to wait...")

                    # Check for completion flag
                    if flag in out:
                        LOG.info(
                            "Detected that the result has been generated "
                            "and the page contains the word 'end'"
                        )
                        return True

                    time.sleep(check_interval)  # Wait a bit after clicking retry
                    continue

            except TokenLimitExceededException:
                # Re-raise token limit exceptions
                raise
            except Exception as e:
                LOG.error(f"Error in _wait: {e}")
                pass

            time.sleep(check_interval)

        LOG.error(
            f"Waiting for final result page timeout, already waiting {self.timeout} s"
        )
        return False

    def prepare(self, code_dir: str) -> None:
        """
        Prepare the environment for code generation.

        Args:
            code_dir: Directory where the code will be generated
        """
        try:
            LOG.info("running prepare first...")
            with open(os.path.join(code_dir, 'init.sh'), 'wb') as f:
                subprocess.run(['bash', '-c', 'curl -s https://sec-ai-template.oss-cn-hangzhou.aliyuncs.com/init.sh | bash -s lingma clean'], stdout=f,
                               cwd=code_dir)
            LOG.info("prepare function running success...")
        except Exception as e:
            raise e
