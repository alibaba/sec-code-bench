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

import atexit
import logging
import threading
import uuid
from queue import Full, Queue
from typing import Dict, Optional, Type

from evaluate.base_consumer import BaseConsumer, BaseConsumerManager
from evaluate.request_job import RequestJob
from utils.basic_utils import get_timestamp
from utils.llm_utils import create_llm_client
from utils.rate_limiter import RateLimiter

logger = logging.getLogger(__name__)


class ResourceManager:
    """This class implements a thread-safe singleton pattern keyed by the client key,
    which ensures that each client configuration has only one manager instance.

    It also manages the lifecycle of LLM clients, including bounded queues to make sure proper rate limiting,
    resource cleanup on shutdown, and fault-tolerant error handling with detailed logging.
    """

    _instances: Dict[str, "ResourceManager"] = {}
    _lock = threading.RLock()

    def __new__(
        cls,
        *,
        model_key: str,
        api_key: str,
        base_url: str,
        thread_count: Optional[int] = None,
        rate_limiter: Optional[RateLimiter] = None,
        timeout: Optional[int] = 10,
        retry_count: Optional[int] = 3,
        retry_factor: Optional[float] = 2.0,
        enable_thinking: Optional[bool] = False,
        enable_streaming: Optional[bool] = False,
        consumer_manager_cls: Optional[Type[BaseConsumerManager]] = BaseConsumerManager,
    ) -> "ResourceManager":
        """singleton with thread-safe"""
        if model_key in cls._instances:
            return cls._instances[model_key]

        with cls._lock:
            if model_key not in cls._instances:
                instance = super(ResourceManager, cls).__new__(cls)
                instance._initialize_instance(
                    model_key=model_key,
                    api_key=api_key,
                    base_url=base_url,
                    thread_count=thread_count,
                    rate_limiter=rate_limiter,
                    timeout=timeout,
                    retry_count=retry_count,
                    retry_factor=retry_factor,
                    enable_thinking=enable_thinking,
                    enable_streaming=enable_streaming,
                    consumer_manager_cls=consumer_manager_cls,
                )
                cls._instances[model_key] = instance

            return cls._instances[model_key]

    def _initialize_instance(
        self,
        model_key: str,
        api_key: str,
        base_url: str,
        thread_count: Optional[int] = None,
        rate_limiter: Optional[RateLimiter] = None,
        timeout: Optional[int] = 10,
        retry_count: Optional[int] = 3,
        retry_factor: Optional[float] = 2.0,
        enable_thinking: Optional[bool] = False,
        enable_streaming: Optional[bool] = False,
        consumer_manager_cls: Optional[Type[BaseConsumerManager]] = BaseConsumerManager,
    ):
        """initialize instance"""
        self.model_key = model_key

        self.llm_client = create_llm_client(api_key, base_url, timeout=timeout)

        self._rate_limiter = rate_limiter

        # Consumer
        self._request_queue = Queue(100_000)
        self._consumer_manager = consumer_manager_cls(
            llm_client=self.llm_client,
            request_queue=self._request_queue,
            rate_limiter=self._rate_limiter,
            max_retries=retry_count,
            retry_factor=retry_factor,
            enable_thinking=enable_thinking,
            enable_streaming=enable_streaming,
        )

        self._consumers = []

        for i in range(thread_count):
            eval_consumer = BaseConsumer(
                identifier=f"consumer_{model_key}_{i}",
                consumer_manager=self._consumer_manager,
            )
            eval_consumer.start()
            self._consumers.append(eval_consumer)

        # self.rate_limit_config = rate_limit_config

        atexit.register(self.shutdown)

        logger.info(
            f"EvalResourceManager initialized with model_key: {model_key} | "
            f"thread_count: {thread_count} | "
            f"retry_count: {retry_count}"
        )

    def generate_and_add_job(
        self, job_properties: dict, job_clz: Type[RequestJob]
    ) -> bool:
        new_job = {
            "timestamp": get_timestamp(),
            "id": str(uuid.uuid4()),
            **job_properties,
        }

        try:
            constructed_job = job_clz(**new_job)
            self._request_queue.put(constructed_job)
            logger.debug(
                f"Queue: Enqueued request job for {constructed_job.job_type} processing | request_id={constructed_job.id}"
            )
        except Full:
            logger.warning(
                f"Queue: Request queue is full. Failed to process request_id={constructed_job.id} for job_type={constructed_job.job_type}. Consider increasing queue capacity."
            )
            return False
        except Exception as e:
            logger.error(
                f"Queue: Failed to enqueue request job {constructed_job.job_type} with request_id={constructed_job.id} for processing. Error: {str(e)}"
            )
            return False
        return True

    @classmethod
    def reset(cls):
        with cls._lock:
            cls._instances.clear()

    def _stop_and_join_all_consumers(self):
        logger.debug(
            f"Shutdown not implemented: Waiting for {len(self._consumers)} consumer thread(s) to complete processing"
        )

        for consumer in self._consumers:
            consumer.pause()

        for consumer in self._consumers:
            try:
                consumer.join()
            except Exception as e:
                logger.error(
                    f"Error joining consumer thread: {e}. Maybe consumer is not running."
                )

            logger.debug(
                f"Shutdown: Consumer thread #{consumer._identifier} successfully terminated"
            )

    def flush_queue(self):
        logger.debug("Flushing EvalResourceManager queues")
        self._request_queue.join()
        logger.debug("Successfully flushed all items inside request queue")

    def shutdown(self):
        atexit.unregister(self.shutdown)

        self._stop_and_join_all_consumers()
