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

from __future__ import annotations

import asyncio
import time
from typing import Any

from sec_code_bench.utils.logger_utils import Logger

LOG = Logger.get_logger(__name__)


class RateLimiter:
    """
    A token bucket rate limiter implementation for asyncio.

    This rate limiter uses the token bucket algorithm to control the rate
    of operations. Tokens are added to the bucket at a constant rate, and
    each operation consumes one token. When there are no tokens available,
    operations will wait until tokens are replenished.

    Attributes:
        window_seconds: The time window in seconds for rate limiting.
        max_cnts: Maximum number of operations allowed in the time window.
        tokens_per_second: Rate at which tokens are added to the bucket.
        burst_size: Maximum number of tokens that can be accumulated.
        tokens: Current number of tokens in the bucket.
        last_refill_time: Timestamp of the last token refill operation.
        _lock: Async lock to ensure thread safety.
    """

    def __init__(
        self,
        max_cnts: int = 60,
        window_seconds: float = 60,
        burst_size: int | None = None,
    ) -> None:
        """
        Initialize the rate limiter.

        Args:
            max_cnts: Maximum number of operations allowed in the time window.
            window_seconds: The time window in seconds for rate limiting.
            burst_size: Maximum number of operations that can be performed in a burst.
                       If None, defaults to max_cnts.

        Raises:
            ValueError: If window_seconds or max_cnts is not positive.
        """
        if window_seconds <= 0:
            raise ValueError("Window seconds must be positive.")
        if max_cnts <= 0:
            raise ValueError("max_cnts must be positive.")
        self.window_seconds: float = window_seconds
        self.max_cnts: int = max_cnts
        self.tokens_per_second: float = max_cnts / window_seconds
        self.burst_size: int = burst_size if burst_size is not None else max_cnts
        self.tokens: float = float(self.burst_size)
        self.last_refill_time: float = time.time()
        self._lock: asyncio.Lock = asyncio.Lock()

    def _refill_tokens(self) -> None:
        """
        Refill tokens in the bucket based on elapsed time.

        This method calculates how many tokens should be added based on the time
        elapsed since the last refill and adds them to the bucket, up to the burst size.
        """
        now: float = time.time()
        time_passed: float = now - self.last_refill_time
        tokens_to_add: float = time_passed * self.tokens_per_second
        self.tokens = min(float(self.burst_size), self.tokens + tokens_to_add)
        self.last_refill_time = now

    async def acquire(self) -> None:
        """
        Acquire a token from the bucket, waiting if necessary.

        This method will wait until a token is available before returning.
        It ensures that operations are rate-limited according to the
        configured parameters.
        """
        while True:
            async with self._lock:
                self._refill_tokens()
                if self.tokens >= 1:
                    self.tokens -= 1
                    return
                tokens_needed: float = 1 - self.tokens
                wait_time: float = tokens_needed / self.tokens_per_second
            # wait outside of lock
            # LOG.debug(f"Waiting for {wait_time:.4f} seconds")
            await asyncio.sleep(wait_time)

    async def __aenter__(self) -> RateLimiter:
        """
        Async context manager entry.

        Returns:
            RateLimiter: The rate limiter instance.
        """
        await self.acquire()
        return self

    async def __aexit__(self, exc_type: Any, exc_val: Any, exc_tb: Any) -> None:
        """
        Async context manager exit.

        Args:
            exc_type: Exception type if an exception was raised in the context.
            exc_val: Exception value if an exception was raised in the context.
            exc_tb: Exception traceback if an exception was raised in the context.
        """
        pass
