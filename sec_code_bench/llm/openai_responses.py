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

from typing import Any

from typing_extensions import override

import openai
from openai import APIError, APITimeoutError, RateLimitError
from openai.types.responses.response import Response

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

# Default system instructions for Responses API (aligned with openai.py chat completion)
DEFAULT_INSTRUCTIONS = "You are a helpful programming assistant."


def _response_output_text(response: Response) -> str:
    """Extract concatenated text from Responses API response.output.

    Response.output is a list of items; we collect text from items with
    type \"message\" and content parts with type \"output_text\".
    """
    parts: list[str] = []
    for item in response.output:
        if getattr(item, "type", None) == "message":
            content_list = getattr(item, "content", []) or []
            for content in content_list:
                if getattr(content, "type", None) == "output_text":
                    text = getattr(content, "text", None)
                    if text:
                        parts.append(text)
    return "".join(parts)


class OpenAIResponses(LLMBase):
    """Accessing LLM via OpenAI Responses API (responses.create)."""

    def __init__(
        self, config: LLMConfig, rate_limiter: RateLimiter, **kwargs: Any
    ) -> None:
        """Initialize the OpenAI client for Responses API.

        Args:
            config: Configuration for the LLM API
            rate_limiter: Rate limiter instance
            **kwargs: Additional arguments (e.g. extra_body for request body)
        """
        super().__init__(config, rate_limiter)
        self.client = openai.OpenAI(api_key=self.api_key, base_url=self.url)
        self.aclient = openai.AsyncOpenAI(
            api_key=self.api_key,
            base_url=self.url,
            max_retries=0,
        )
        self.kwargs = kwargs

    @override
    async def _aquery_implementation(
        self,
        sys_prompt: str,
        user_prompt: str,
        **kwargs: Any,
    ) -> str:
        """Implementation of async query using OpenAI Responses API.

        Args:
            sys_prompt: System / instructions message
            user_prompt: User message (main input)
            **kwargs: May include "parameters" dict (e.g. reasoning.effort, extra_body).

        Returns:
            Model response text (concatenated from response.output message items).

        Raises:
            LLMTimeoutError: If the request times out
            LLMRateLimitError: If rate limit is exceeded
            LLMAPIError: If there is an API error
            LLMBaseException: For other unexpected errors
        """
        try:
            # Top-level parameters for responses.create() (NOT extra_body)
            TOP_LEVEL_PARAMS = {
                "temperature", "top_p", "reasoning",
                "max_output_tokens", "max_tool_calls",
                "tools", "tool_choice", "parallel_tool_calls",
                "text", "truncation", "user", "store",
                "metadata", "include", "service_tier",
            }

            extra_body = dict(self.kwargs.get("extra_body") or {})
            instructions = sys_prompt.strip() or DEFAULT_INSTRUCTIONS
            create_kwargs: dict[str, Any] = {
                "model": self.model,
                "stream": False,
                "input": user_prompt,
                "instructions": instructions,
            }

            # Separate top-level params from extra_body
            parameters: dict[str, Any] | None = kwargs.get("parameters")
            if parameters and isinstance(parameters, dict):
                for key, value in parameters.items():
                    if key in TOP_LEVEL_PARAMS:
                        if value is not None:
                            create_kwargs[key] = value
                    else:
                        extra_body[key] = value

            if extra_body:
                create_kwargs["extra_body"] = extra_body

            LOG.info(
                f"Responses API request: "
                f"{ {k: v for k, v in create_kwargs.items() if k != 'input'} }"
            )
            response = await self.aclient.responses.create(**create_kwargs)
            return _response_output_text(response) or ""
        except APITimeoutError as e:
            LOG.error(f"Responses API timeout: {e}")
            raise LLMTimeoutError("OpenAI Responses timeout", cause=e) from e
        except RateLimitError as e:
            LOG.error(f"Responses API rate limit: {e}")
            raise LLMRateLimitError(
                "OpenAI Responses rate limit", cause=e
            ) from e
        except APIError as e:
            LOG.error(f"Responses API error: {e}")
            raise LLMAPIError("OpenAI Responses API error", cause=e) from e
        except Exception as e:
            LOG.error(f"Responses API unknown error: {type(e).__name__}: {e}")
            raise LLMBaseException("Unknown error", cause=e) from e

    @override
    def sync_close(self) -> None:
        """Close the sync client connection."""
        self.client.close()

    @override
    async def async_close(self) -> None:
        """Close the async client connection."""
        await self.aclient.close()
