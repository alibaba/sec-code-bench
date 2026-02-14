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

import anthropic
from anthropic import APIError, APITimeoutError, RateLimitError

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

DEFAULT_MAX_TOKENS = 4096


class AnthropicMessages(LLMBase):
    """Accessing LLM via Anthropic Messages API."""

    def __init__(
        self, config: LLMConfig, rate_limiter: RateLimiter, **kwargs: Any
    ) -> None:
        """Initialize the Anthropic client.

        Args:
            config: Configuration for the LLM API
            rate_limiter: Rate limiter instance
            **kwargs: Additional arguments (e.g. extra_body)
        """
        super().__init__(config, rate_limiter)
        self.client = anthropic.Anthropic(
            api_key=self.api_key, base_url=self.url
        )
        self.aclient = anthropic.AsyncAnthropic(
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
        """Implementation of async query using Anthropic Messages API.

        Args:
            sys_prompt: System prompt
            user_prompt: User message
            **kwargs: May include "parameters" dict
                (e.g. max_tokens, temperature, thinking, etc.).

        Returns:
            Model response text.

        Raises:
            LLMTimeoutError: If the request times out
            LLMRateLimitError: If rate limit is exceeded
            LLMAPIError: If there is an API error
            LLMBaseException: For other unexpected errors
        """
        try:
            # Top-level parameters for messages.create()
            TOP_LEVEL_PARAMS = {
                "max_tokens",
                "temperature",
                "top_p",
                "top_k",
                "stop_sequences",
                "tools",
                "tool_choice",
                "thinking",  # extended thinking
                "metadata",
                # "output_config" goes via extra_body (SDK may not have it yet)
            }

            extra_body = dict(self.kwargs.get("extra_body") or {})
            create_kwargs: dict[str, Any] = {
                "model": self.model,
                "max_tokens": DEFAULT_MAX_TOKENS,
                "system": sys_prompt.strip() if sys_prompt.strip() else "You are a helpful assistant.",
                "messages": [
                    {"role": "user", "content": user_prompt},
                ],
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
                f"Anthropic Messages request: "
                f"{ {k: v for k, v in create_kwargs.items() if k not in ('messages', 'system')} }"
            )
            response = await self.aclient.messages.create(**create_kwargs)

            # Debug: log full response structure
            LOG.info(f"Anthropic response type: {type(response)}")
            LOG.info(f"Anthropic response repr: {repr(response)}")

            # Extract text from response
            # Handle both standard Message object and raw string
            # (some proxies return non-standard formats)
            if isinstance(response, str):
                return response
            if hasattr(response, "content"):
                parts: list[str] = []
                for block in response.content:
                    if hasattr(block, "type") and block.type == "text":
                        parts.append(block.text)
                    elif isinstance(block, str):
                        parts.append(block)
                return "".join(parts) or ""
            return str(response)

        except APITimeoutError as e:
            LOG.error(f"Anthropic timeout: {e}")
            raise LLMTimeoutError(
                "Anthropic timeout", cause=e
            ) from e
        except RateLimitError as e:
            LOG.error(f"Anthropic rate limit: {e}")
            raise LLMRateLimitError(
                "Anthropic rate limit", cause=e
            ) from e
        except APIError as e:
            LOG.error(f"Anthropic API error: {e}")
            raise LLMAPIError(
                "Anthropic API error", cause=e
            ) from e
        except Exception as e:
            LOG.error(
                f"Anthropic unknown error: {type(e).__name__}: {e}"
            )
            raise LLMBaseException("Unknown error", cause=e) from e

    @override
    def sync_close(self) -> None:
        """Close the sync client connection."""
        self.client.close()

    @override
    async def async_close(self) -> None:
        """Close the async client connection."""
        await self.aclient.close()
