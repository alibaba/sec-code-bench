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

import asyncio
import inspect
from datetime import datetime
from functools import wraps
from typing import Any, Callable

from loguru import logger
from tzlocal import get_localzone


def get_timestamp():
    local_tz = get_localzone()
    return datetime.now(local_tz)


def log_inout_state(
    func: Callable, input_print: bool = True, output_print: bool = True
) -> Callable:
    if inspect.iscoroutinefunction(func):

        @wraps(func)
        async def async_wrapper(*args, **kwargs) -> Any:
            if input_print:
                logger.debug(
                    f"Calling async function '{func.__name__}' with args {args} and kwargs {kwargs}"
                )
            result = await func(*args, **kwargs)
            if output_print:
                logger.debug(f"Async function '{func.__name__}' returned {result}")
            return result

        return async_wrapper
    else:

        @wraps(func)
        def sync_wrapper(*args, **kwargs) -> Any:
            if input_print:
                logger.debug(
                    f"Calling sync function '{func.__name__}' with args {args} and kwargs {kwargs}"
                )
            result = func(*args, **kwargs)
            if output_print:
                logger.debug(f"Sync function '{func.__name__}' returned {result}")
            return result

        return sync_wrapper


class APIError(Exception):
    def __init__(self, status: int | str, message: str, details: Any = None):
        self.status = status
        self.message = message
        self.details = details

    def __str__(self):
        msg = f"APIError: {self.status} {self.message} - {self.details}"
        return msg


class ValidateError(Exception):
    def __init__(self, valid: bool, required: bool, message: str):
        self.valid = valid
        self.required = required
        self.message = message

    def __str__(self):
        return f"ValidateError: {self.valid}, required: {self.required}, message: {self.message}"


# semaphore = asyncio.Semaphore(int(os.environ.get("GIT_RATE_LIMIT", 15)))


async def global_rlimit_wrap(func, semaphore: asyncio.Semaphore):
    async with semaphore:
        return await func
