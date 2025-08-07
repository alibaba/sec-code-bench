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

# This FastAPI-based service is used to monitor and record access to specific URL paths during dynamic security assessments.

# Endpoint Description:
# - `/log/{path}`: Records access to the specified path for later inspection.
# - `/check?path={path}`: Retrieves the access history of the specified path for security risk assessment.

# It runs on port 7000 by default.
# This script must be executed prior to running the eval_dynamic.py script.

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Dict, List, Optional
import time
import multiprocessing
import os
import signal
import logging

LOG: logging.Logger = logging.getLogger(__name__)


class SecurityMonitor:
    class LogResponse(BaseModel):
        path: str
        timestamp: float
        message: str

    class CheckResponse(BaseModel):
        path: str
        records: List[dict]
        total_count: int

    def __init__(self, host="0.0.0.0", port=7000, cleanup_threshold=60):
        self._app = FastAPI(title="Simple HTTP Log Service", version="1.0.0")
        self._host = host
        self._port = port
        self._log_records: Dict[str, List[tuple]] = {}
        self._lock = multiprocessing.Lock()
        self._cleanup_threshold = cleanup_threshold
        self._process = None

        # 绑定路由
        self._app.get("/log/{path:path}")(self._log_path)
        self._app.get("/check")(self._check_path)

    def _cleanup_expired_records(self):
        current_time = time.time()
        with self._lock:
            expired_paths = []
            for path, records in self._log_records.items():
                valid_records = [(ts, count) for ts, count in records if current_time - ts <= self._cleanup_threshold]
                if valid_records:
                    self._log_records[path] = valid_records
                else:
                    expired_paths.append(path)
            for path in expired_paths:
                del self._log_records[path]

    def _add_log_record(self, path: str):
        current_time = time.time()
        with self._lock:
            if path not in self._log_records:
                self._log_records[path] = []
            if self._log_records[path] and self._log_records[path][-1][0] == current_time:
                last_timestamp, last_count = self._log_records[path][-1]
                self._log_records[path][-1] = (last_timestamp, last_count + 1)
            else:
                self._log_records[path].append((current_time, 1))

    async def _log_path(self, path: str):
        self._add_log_record(path)
        self._cleanup_expired_records()
        return self.LogResponse(
            path=path,
            timestamp=time.time(),
            message=f"Path '{path}' has been logged"
        )

    async def _check_path(self, path: Optional[str] = None):
        if not path:
            raise HTTPException(status_code=400, detail="Path parameter is required")
        self._cleanup_expired_records()
        with self._lock:
            if path not in self._log_records:
                raise HTTPException(status_code=404, detail=f"No records found for path '{path}'")
            records = self._log_records[path]
            now = time.time()
            formatted_records = [
                {
                    "timestamp": ts,
                    "count": count,
                    "time_ago": now - ts
                }
                for ts, count in records
            ]
            total_count = sum(count for _, count in records)
            return self.CheckResponse(
                path=path,
                records=formatted_records,
                total_count=total_count
            )

    def start(self):
        if self._process and self._process.is_alive():
            LOG.info("Monitor already running.")
            return
        self._process = multiprocessing.Process(
                target=_start_server,
                args=(self._host, self._port),
                daemon=True
            )
        self._process.start()
        LOG.info(f"SecurityMonitor started on http://{self._host}:{self._port}")

    def stop(self):
        if self._process is not None and self._process.is_alive():
            LOG.info("Stopping SecurityMonitor...")
            os.kill(self._process.pid, signal.SIGTERM)
            self._process.join(3)
            self._process = None
            LOG.info("SecurityMonitor stopped.")

def _start_server(host, port):
    import uvicorn
    monitor = SecurityMonitor(host, port)
    uvicorn.run(
        monitor._app,
        host=host,
        port=port,
        log_level="info",
        lifespan="off"
    )
# if __name__ == "__main__":
#     import time
#     monitor = SecurityMonitor()
#     monitor.start()
#     try:
#         while True:
#             time.sleep(1)
#     except KeyboardInterrupt:
#         monitor.stop()
