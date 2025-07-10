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
import threading
from queue import Empty, Queue
from typing import Callable, Optional, ParamSpec, TypeVar

import backoff
import httpx
import requests
from loguru import logger
from openai import OpenAI

from evaluate.request_job import RequestJob
from utils.basic_utils import APIError
from utils.rate_limiter import RateLimiter

T = TypeVar("T")
P = ParamSpec("P")


class BaseConsumerManager:
    def __init__(
        self,
        llm_client: OpenAI,
        request_queue: Queue,
        rate_limiter: RateLimiter,
        max_retries: int,
        retry_factor: float,
        enable_thinking: Optional[bool] = False,
        enable_streaming: Optional[bool] = False,
    ):
        self._llm_client = llm_client
        self._queue = request_queue
        self._max_retries = max_retries
        self._rate_limiter = rate_limiter
        self._retry_factor = retry_factor
        self._enable_thinking = enable_thinking
        self._enable_streaming = enable_streaming

    def process_next_request(self):
        with self._rate_limiter:
            try:
                request_job: RequestJob = self._queue.get(block=True, timeout=1)
                logger.debug(
                    f"Consumer: Processing evaluation of model={request_job.model} for request_id={request_job.id} of job type {request_job.job_type}"
                )
                if self._validate_job(job=request_job):
                    self._process_request_job(job=request_job)
                else:
                    logger.warning(
                        f"Consumer: Invalid job {request_job.job_type} with request_id={request_job.id}. Skipping processing."
                    )

                self._queue.task_done()
            except Empty:
                logger.debug("Queue: Request queue is empty, waiting for new jobs")
                pass
            except Exception as e:
                import traceback

                logger.error(
                    f"Consumer: Failed to process evaluation due to unexpected error. Queue item marked as done. Error: {e}\n{traceback.format_exc()}"
                )
                self._queue.task_done()

    def _process_request_job(self, *, job):
        raise NotImplementedError(
            "Subclass must implement this method for processing request job with llm_client."
        )

    def _validate_job(self, *, job) -> bool:
        raise NotImplementedError(
            "Subclass must implement this method for validating request job."
        )

    def _request_with_backoff(
        self, func: Callable[P, T], *args: P.args, **kwargs: P.kwargs
    ):

        def _check_acceptable_error(e: Exception) -> bool:
            if isinstance(e, APIError):
                return (
                    e.status is not None and 400 <= e.status < 500 and e.status != 429
                )
            if isinstance(e, requests.exceptions.RequestException):
                return (
                    e.response is not None
                    and e.response.status_code < 500
                    and e.response.status_code != 429
                )
            if isinstance(e, httpx.HTTPStatusError):
                return (
                    e.response is not None
                    and e.response.status_code < 500
                    and e.response.status_code != 429
                )  # giveup when encountering non-429 error
            return False

        @backoff.on_exception(
            backoff.expo,
            Exception,
            max_tries=self._max_retries,
            factor=self._retry_factor,
            giveup=_check_acceptable_error,
        )
        def exec_job_with_backoff() -> T:
            return func(*args, **kwargs)

        @backoff.on_exception(
            backoff.expo,
            Exception,
            max_tries=self._max_retries,
            factor=self._retry_factor,
            giveup=_check_acceptable_error,
        )
        async def exec_job_with_backoff_async() -> T:
            return await func(*args, **kwargs)

        return (
            exec_job_with_backoff_async()
            if asyncio.iscoroutinefunction(func)
            else exec_job_with_backoff()
        )


class BaseConsumer(threading.Thread):

    _identifier: str
    _max_retries: int
    _consumer_manager: BaseConsumerManager

    def __init__(self, identifier: str, consumer_manager: BaseConsumerManager):
        super().__init__()
        self.daemon = True
        self.enable = True

        self._identifier = identifier
        self._consumer_manager = consumer_manager

    def run(self):
        logger.info(
            f"Consumer: Starting EvalConsumer thread with identifier {self._identifier}"
        )
        while self.enable:
            self._consumer_manager.process_next_request()

    def pause(self):
        logger.info(
            f"Consumer: Pausing EvalConsumer thread with identifier {self._identifier}"
        )
        self.enable = False
