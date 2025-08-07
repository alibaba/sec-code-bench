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

import collections
import threading
import time
from typing import Callable, Optional, Dict, Any

from loguru import logger

import asyncio

import logging
LOG: logging.Logger = logging.getLogger(__name__)

class AsyncRateLimiter:
    def __init__(
        self,
        max_cnts,
        window_seconds=60,
        burst_size=None,
    ):
        if window_seconds <= 0:
            raise ValueError("Window seconds must be positive.")
        if max_cnts <= 0:
            raise ValueError("max_cnts must be positive.")
        self.window_seconds = window_seconds
        self.max_cnts = max_cnts
        self.tokens_per_second = max_cnts / window_seconds
        self.burst_size = burst_size if burst_size is not None else max_cnts
        self.tokens = self.burst_size
        self.last_refill_time = time.time()
        self._lock = asyncio.Lock()

    def _refill_tokens(self):
        now = time.time()
        time_passed = now - self.last_refill_time
        tokens_to_add = time_passed * self.tokens_per_second
        self.tokens = min(self.burst_size, self.tokens + tokens_to_add)
        self.last_refill_time = now

    async def acquire(self):
        while True:
            async with self._lock:
                self._refill_tokens()
                if self.tokens >= 1:
                    self.tokens -= 1
                    return
                tokens_needed = 1 - self.tokens
                wait_time = tokens_needed / self.tokens_per_second
            # wait outside of lock
            LOG.debug(f"Waiting for {wait_time:.4f} seconds")
            await asyncio.sleep(wait_time)

    async def __aenter__(self):
        await self.acquire()
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        pass



class RateLimiter(object):

    def __init__(
        self,
        max_cnts,
        window_seconds=60,
        callback: Optional[Callable[[float], None]] = None,
    ):
        if window_seconds <= 0:
            raise ValueError("The windows for rate limiting should be > 0")

        if max_cnts <= 0:
            raise ValueError("The number of requests should be > 0")

        self.requests = collections.deque()

        self.window_seconds = window_seconds
        self.max_cnts = max_cnts
        self.callback = callback
        self._lock = threading.Lock()

    def _wait_for_capacity(self):
        end_ts = time.time() + self.window_seconds - self._timespan
        if self.callback:
            t = threading.Thread(target=self.callback, args=(end_ts,))
            t.daemon = True
            t.start()
        sleeptime = end_ts - time.time()
        # sleep until the next window
        if sleeptime > 0:
            time.sleep(sleeptime)

    def __enter__(self):
        with self._lock:
            if len(self.requests) >= self.max_cnts:
                self._wait_for_capacity()
            return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        with self._lock:
            self.requests.append(time.time())

            while self._timespan >= self.window_seconds:
                self.requests.popleft()

    @property
    def _timespan(self):
        return self.requests[-1] - self.requests[0]
