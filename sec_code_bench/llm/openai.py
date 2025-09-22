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

from typing import Any, override

import openai
from openai import APIError, APITimeoutError, RateLimitError

from sec_code_bench.llm.llm_base import (
    LLMAPIError,
    LLMBase,
    LLMBaseException,
    LLMConfig,
    LLMRateLimitError,
    LLMTimeoutError,
)
from sec_code_bench.utils.logger_utils import Logger
from sec_code_bench.utils.rate_limiter import RateLimiter

LOG = Logger.get_logger(__name__)

DEFAULT_TEMPERATURE = 0.6
DEFAULT_TOP_P = 0.9


class OPENAI(LLMBase):
    """Accessing LLM In OPENAI Format"""

    def __init__(
        self, config: LLMConfig, rate_limiter: RateLimiter, **kwargs: Any
    ) -> None:
        """Initialize the OpenAI client.

        Args:
            config: Configuration for the LLM API
            rate_limiter: Rate limiter instance
            **kwargs: Additional arguments for the client
        """
        super().__init__(config, rate_limiter)
        self.client = openai.OpenAI(api_key=self.api_key, base_url=self.url)
        self.aclient = openai.AsyncOpenAI(
            api_key=self.api_key,
            base_url=self.url,
            max_retries=0,  # Do not auto-retry
        )
        self.kwargs = kwargs

    @override
    async def _aquery_implementation(
        self,
        prompt: str,
        temperature: float = DEFAULT_TEMPERATURE,
        top_p: float = DEFAULT_TOP_P,
    ) -> str:
        """Implementation of async query for OpenAI API.

        Args:
            prompt: Input prompt
            temperature: Sampling temperature
            top_p: Top-p sampling parameter

        Returns:
            Model response result

        Raises:
            LLMTimeoutError: If the request times out
            LLMRateLimitError: If rate limit is exceeded
            LLMAPIError: If there is an API error
            LLMBaseException: For other unexpected errors
        """
        try:
            response = await self.aclient.chat.completions.create(
                model=self.model,
                stream=False,
                messages=[
                    {
                        "role": "system",
                        "content": """TODO""",
                    },
                    {"role": "user", "content": prompt},
                ],
                temperature=temperature,
                top_p=top_p,
                extra_body=self.kwargs.get("extra_body", {}),
            )
            content = response.choices[0].message.content
            return content or ""  # Ensure non-null return
        except APITimeoutError as e:  # Catch subclass first
            raise LLMTimeoutError("OpenAI timeout", cause=e) from e
        except RateLimitError as e:  # Then catch other subclasses
            raise LLMRateLimitError("OpenAI rate limit", cause=e) from e
        except APIError as e:  # Finally catch base class
            raise LLMAPIError("OpenAI API error", cause=e) from e
        except Exception as e:  # Catch all other exceptions
            raise LLMBaseException("Unknown error", cause=e) from e

    @override
    def sync_close(self) -> None:
        """Close the sync client connection."""
        self.client.close()

    @override
    async def async_close(self) -> None:
        """Close the async client connection."""
        await self.aclient.close()
