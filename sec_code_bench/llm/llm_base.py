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

from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Any

from tenacity import retry, retry_if_exception_type, stop_after_attempt
from tenacity.wait import wait_base

from sec_code_bench.utils.logger_utils import Logger
from sec_code_bench.utils.rate_limiter import RateLimiter

LOG = Logger.get_logger(__name__)


@dataclass
class LLMConfig:
    """Configuration class for LLM API connections."""

    model: str
    url: str
    api_key: str | None = None


class wait_for_rate_limit(wait_base):
    """Custom wait function that checks for retry-after header
    in RateLimitError for sync methods"""

    def __init__(self, fallback_wait: float = 3.0) -> None:
        """Initialize the wait function with a fallback wait time.

        Args:
            fallback_wait: Default wait time in seconds
                when no retry-after header is found
        """
        self.fallback_wait = fallback_wait

    def __call__(self, retry_state: Any) -> float:
        """Calculate wait time based on retry state and headers.

        Args:
            retry_state: The current retry state

        Returns:
            Wait time in seconds
        """
        # Check if there's an exception in the outcome
        if retry_state.outcome is not None and retry_state.outcome.failed:
            try:
                # Get the exception from the outcome
                exception = retry_state.outcome.exception()

                # Handle case where exception is None
                if exception is None:
                    # Use exponential backoff with max 10 seconds,
                    # starting from 1 seconds
                    return min(10.0, (1 * (2**retry_state.attempt_number)))

                # Check if it's a RateLimitError with retry-after header
                if hasattr(exception, "response") and exception.response is not None:
                    # Check for retry-after header (case insensitive)
                    retry_after = exception.response.headers.get("retry-after")
                    if retry_after is not None:
                        try:
                            # Try to parse the retry-after value as a number
                            return float(retry_after)
                        except (ValueError, TypeError):
                            # If parsing fails, fall back to exponential backoff
                            LOG.warning(
                                f"Could not parse retry-after value: {retry_after}"
                            )

                # If we can't get retry-after from headers,
                # check for other common rate limit headers
                if hasattr(exception, "response") and exception.response is not None:
                    # Check for other common rate limit headers
                    for header_name in [
                        "Retry-After",
                        "X-RateLimit-Reset",
                        "X-Rate-Limit-Reset",
                    ]:
                        retry_after = exception.response.headers.get(header_name)
                        if retry_after is not None:
                            try:
                                return float(retry_after)
                            except (ValueError, TypeError):
                                LOG.warning(
                                    f"Could not parse {header_name}: {retry_after}"
                                )

            except Exception as e:
                LOG.warning(f"Error while processing retry exception: {e}")

        # Fallback to exponential backoff with max 10 seconds, starting from 2 seconds
        sleep = min(10.0, (1 * (2**retry_state.attempt_number)))
        LOG.info(f"Sleeping for {sleep} seconds")
        return sleep


"""Custom exceptions for LLM API interfaces."""


class LLMBaseException(Exception):
    """Base exception class for LLM API errors."""

    def __init__(self, message: str, cause: Exception | None = None) -> None:
        """Initialize the base exception.

        Args:
            message: Error message
            cause: Underlying exception that caused this error
        """
        super().__init__(message)
        self.message = message
        self.cause = cause

    def __str__(self) -> str:
        """String representation of the exception."""
        if self.cause:
            return f"{self.message} (caused by: {str(self.cause)})"
        return self.message


class LLMAPIError(LLMBaseException):
    """Exception for LLM API errors."""

    def __init__(
        self,
        message: str,
        status_code: int | None = None,
        cause: Exception | None = None,
    ) -> None:
        """Initialize the API error exception.

        Args:
            message: Error message
            status_code: HTTP status code if applicable
            cause: Underlying exception that caused this error
        """
        super().__init__(message, cause)
        self.status_code = status_code


class LLMRateLimitError(LLMBaseException):
    """Exception for LLM rate limit errors."""

    def __init__(
        self,
        message: str,
        retry_after: float | None = None,
        cause: Exception | None = None,
    ) -> None:
        """Initialize the rate limit exception.

        Args:
            message: Error message
            retry_after: Time to wait before retrying in seconds
            cause: Underlying exception that caused this error
        """
        super().__init__(message, cause)
        self.retry_after = retry_after


class LLMTimeoutError(LLMBaseException):
    """Exception for LLM timeout errors."""

    def __init__(
        self,
        message: str,
        timeout: float | None = None,
        cause: Exception | None = None,
    ) -> None:
        """Initialize the timeout exception.

        Args:
            message: Error message
            timeout: Timeout duration in seconds
            cause: Underlying exception that caused this error
        """
        super().__init__(message, cause)
        self.timeout = timeout


class LLMBase(ABC):
    """Abstract base class for LLM APIs."""

    def __init__(self, config: LLMConfig, rate_limit: RateLimiter) -> None:
        """Initialize the LLM API client.

        Args:
            config: Configuration for the LLM API
            rate_limit: Rate limiter instance
        """
        self.model = config.model
        self.api_key = config.api_key
        self.url = config.url
        self.rate_limit = rate_limit
        self._is_closed = False

    @retry(
        retry=retry_if_exception_type((LLMAPIError, LLMRateLimitError, LLMTimeoutError)),
        stop=stop_after_attempt(5),  # Maximum 5 retry attempts
        wait=wait_for_rate_limit(),  # exponential backoff
        reraise=True,  # Re-raise the exception after all retries
        before=Logger.log_before,
    )
    async def aquery(self, prompt: str, **kwargs: Any) -> str:
        """Asynchronous query method with rate limiting

        Args:
            prompt: Input prompt
            **kwargs: Additional arguments for the query implementation

        Returns:
            Model response result
        """
        # Use context manager for rate limiting
        async with self.rate_limit:
            # Call the specific implementation
            return await self._aquery_implementation(prompt, **kwargs)

    # Need to encapsulate exceptions and return for capture
    @abstractmethod
    async def _aquery_implementation(self, prompt: str, **kwargs: Any) -> str:
        """Specific async query implementation to be provided by subclasses

        Args:
            prompt: Input prompt
            **kwargs: Additional arguments for the query implementation

        Returns:
            Model response result
        """
        pass

    @abstractmethod
    def sync_close(self) -> None:
        """Abstract method for sync closing connections"""
        pass

    @abstractmethod
    async def async_close(self) -> None:
        """Abstract method for async closing connections"""
        pass

    @staticmethod
    def response_json_format(response: str) -> str:
        """Extract the first complete JSON object string.

        Args:
            response: Raw response string

        Returns:
            Extracted JSON string or original response if no JSON found
        """
        # Extract the first complete JSON object string
        start, end = response.find("{"), response.rfind("}")
        if start != -1 and end != -1 and end > start:
            return response[start : end + 1]
        return response
