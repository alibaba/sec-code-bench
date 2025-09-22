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


# Dynamic Security Assessment Assistant - Path Access Monitor

# This FastAPI-based service is used to monitor and record access to specific URL paths
# during dynamic security assessments.

# Endpoint Description:
# - `/log/{path}`: Records access to the specified path for later inspection.
# - `/check?path={path}`: Retrieves the access history of
#    the specified path for security risk assessment.

# It runs on port 7000 by default.
# This script must be executed prior to running the eval_dynamic.py script.

import asyncio
import multiprocessing
import os
import signal
import time

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from sec_code_bench.utils.logger_utils import Logger

LOG = Logger.get_logger(__name__)


class SecurityMonitor:
    class LogResponse(BaseModel):
        """Response model for logging path access."""

        path: str
        timestamp: float
        message: str

    class CheckResponse(BaseModel):
        """Response model for checking path access records."""

        path: str
        records: list[dict]
        total_count: int

    def __init__(
        self,
        host: str = "0.0.0.0",
        port: int = 7000,
        cleanup_threshold: int = 60,
    ) -> None:
        """Initialize the SecurityMonitor.

        Args:
            host: Host address to bind the server to
            port: Port number to listen on
            cleanup_threshold: Time threshold for cleaning up records in seconds
        """
        self._app = FastAPI(title="Simple HTTP Log Service", version="1.0.0")
        self._host = host
        self._port = port
        self._log_records: dict[str, list[tuple[float, int]]] = {}
        self._lock = multiprocessing.Lock()
        self._cleanup_threshold = cleanup_threshold
        self._process: multiprocessing.Process | None = None
        self._startup_event = multiprocessing.Event()

        # Bind routes
        self._app.get("/log/{path:path}")(self._log_path)
        self._app.get("/check")(self._check_path)

    def _cleanup_expired_records(self) -> None:
        """Clean up expired log records based on the cleanup threshold.

        Removes records that are older than the cleanup threshold from the log records.
        """
        current_time = time.time()
        with self._lock:
            expired_paths = []
            for path, records in self._log_records.items():
                valid_records = [
                    (ts, count)
                    for ts, count in records
                    if current_time - ts <= self._cleanup_threshold
                ]
                if valid_records:
                    self._log_records[path] = valid_records
                else:
                    expired_paths.append(path)
            for path in expired_paths:
                del self._log_records[path]

    def _add_log_record(self, path: str) -> None:
        """Add a log record for the specified path.

        Args:
            path: The path to log access for
        """
        current_time = time.time()
        with self._lock:
            if path not in self._log_records:
                self._log_records[path] = []
            if self._log_records[path] and self._log_records[path][-1][0] == current_time:
                last_timestamp, last_count = self._log_records[path][-1]
                self._log_records[path][-1] = (last_timestamp, last_count + 1)
            else:
                self._log_records[path].append((current_time, 1))

    async def _log_path(self, path: str) -> LogResponse:
        """Log access to a specific path.

        Args:
            path: The path being accessed

        Returns:
            LogResponse: Response containing path, timestamp and message
        """
        self._add_log_record(path)
        self._cleanup_expired_records()
        return self.LogResponse(
            path=path,
            timestamp=time.time(),
            message=f"Path '{path}' has been logged",
        )

    async def _check_path(self, path: str | None = None) -> CheckResponse:
        """Check access records for a specific path.

        Args:
            path: The path to check records for

        Returns:
            CheckResponse: Response containing path, records and total count

        Raises:
            HTTPException: If path parameter is missing or no records found for path
        """
        if not path:
            raise HTTPException(status_code=400, detail="Path parameter is required")
        self._cleanup_expired_records()
        with self._lock:
            if path not in self._log_records:
                raise HTTPException(
                    status_code=404,
                    detail=f"No records found for path '{path}'",
                )
            records = self._log_records[path]
            now = time.time()
            formatted_records = [
                {"timestamp": ts, "count": count, "time_ago": now - ts}
                for ts, count in records
            ]
            total_count = sum(count for _, count in records)
            return self.CheckResponse(
                path=path, records=formatted_records, total_count=total_count
            )

    def start(self) -> None:
        """Start the security monitor server in a separate process.

        Starts the FastAPI server in a multiprocessing process if not already running.
        """
        if self._process and self._process.is_alive():
            LOG.info("Monitor already running.")
            return
        self._process = multiprocessing.Process(
            target=_start_server,
            args=(self._host, self._port, self._startup_event),
            daemon=True,
        )
        self._process.start()
        LOG.info(f"SecurityMonitor started on http://{self._host}:{self._port}")

    async def wait_for_startup(self, timeout: float = 10.0) -> bool:
        """Asynchronously wait for the server to finish starting up.

        Args:
            timeout: Time to wait for startup completion in seconds

        Returns:
            bool: True if server started successfully, False if timed out
        """
        loop = asyncio.get_event_loop()
        try:
            # Wait for event in a separate thread to avoid blocking the event loop
            await loop.run_in_executor(None, self._wait_for_startup_sync, timeout)
            return True
        except Exception as e:
            LOG.error(f"Server startup timeout: {e}")
            return False

    def _wait_for_startup_sync(self, timeout: float) -> None:
        """Synchronously wait for the server to finish starting up.

        Args:
            timeout: Time to wait for startup completion in seconds

        Raises:
            TimeoutError: If server does not start within the specified timeout
        """
        if not self._startup_event.wait(timeout):
            raise TimeoutError("SecurityMonitor startup timeout")

    def stop(self) -> None:
        """Stop the security monitor server process.

        Terminates the running process if it exists and is alive.
        """
        if self._process is not None and self._process.is_alive():
            LOG.info("Stopping SecurityMonitor...")
            os.kill(self._process.pid, signal.SIGTERM)
            self._process.join(3)
            self._process = None
            LOG.info("SecurityMonitor stopped.")


def _start_server(host: str, port: int, startup_event: multiprocessing.Event) -> None:
    """Start the uvicorn server for the security monitor.

    Args:
        host: Host address to bind the server to
        port: Port number to listen on
        startup_event: Event to signal when server has started
    """
    import threading

    import uvicorn

    monitor = SecurityMonitor(host, port)

    # Start a thread to set the event after the server starts
    def set_startup_event() -> None:
        # Wait a bit to ensure the server starts
        time.sleep(0.5)
        startup_event.set()

    # Set the event after the server starts
    threading.Thread(target=set_startup_event, daemon=True).start()

    uvicorn.run(monitor._app, host=host, port=port, log_level="info", lifespan="off")
