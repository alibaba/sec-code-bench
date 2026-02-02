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
        user_prompt: str,
        **kwargs: Any
    ) -> str:
        """Implementation of async query for OpenAI API.

        Args:
            user_prompt: User prompt
            parameters: Optional parameters dict to pass to the API call.
                Parameters like "reasoning_effort", "max_tokens", etc. will be
                placed at the top level of the request. Other parameters will be
                merged into extra_body. Examples:
                - {"reasoning_effort": "high"} -> top level
                - {"enable_thinking": True} -> extra_body

        Returns:
            Model response result

        Raises:
            LLMTimeoutError: If the request times out
            LLMRateLimitError: If rate limit is exceeded
            LLMAPIError: If there is an API error
            LLMBaseException: For other unexpected errors
        """
        try:
            # Parameters that should be placed at the top level of the request
            # instead of in extra_body.
            # These are standard OpenAI API parameters supported in version 1.108.1.
            # Note: Some parameters may be model-specific (e.g., reasoning_effort for o1 models,
            # max_completion_tokens for o1 models instead of max_tokens).
            TOP_LEVEL_PARAMS = {
                # Standard parameters (supported by most models)
                "max_tokens",
                "max_completion_tokens",
                "presence_penalty",
                "frequency_penalty",
                "logit_bias",
                "logprobs",
                "top_logprobs",
                "n",
                "seed",
                "stop",
                "tools",
                "tool_choice",
                "response_format",
                "user",
                "parallel_tool_calls",
                # Model-specific parameters
                "reasoning_effort",  # For reasoning models (e.g., "low", "medium", "high")
                # Sampling parameters (can be overridden or excluded via parameters)
                # Use null in parameters to exclude, e.g., {"top_p": null}
                "temperature",
                "top_p",
            }

            # Merge parameters with extra_body if both are provided
            extra_body = self.kwargs.get("extra_body", {}).copy()
            
            # Build the request kwargs
            # temperature and top_p are set to default values here,
            # but can be overridden or excluded via parameters later.
            request_kwargs = {
                "model": self.model,
                "stream": False,
                "messages": [
                    {
                        "role": "system",
                        "content": "You are a professional programmer.",
                    },
                    {
                        "role": "user",
                        "content": user_prompt
                    },
                ],
                "temperature": DEFAULT_TEMPERATURE,
                "top_p": DEFAULT_TOP_P,
            }

            # Process parameters: separate top-level params from extra_body params
            parameters: dict[str, Any] | None = kwargs.get("parameters")
            if parameters is not None and isinstance(parameters, dict):
                for key, value in parameters.items():
                    if key in TOP_LEVEL_PARAMS:
                        if value is None:
                            # Remove the parameter if value is None
                            # e.g., {"top_p": null} will exclude top_p from request
                            request_kwargs.pop(key, None)
                        else:
                            # Place at top level of request
                            request_kwargs[key] = value
                    else:
                        # Place in extra_body
                        extra_body[key] = value

            # Only add extra_body if it's not empty
            if extra_body:
                request_kwargs["extra_body"] = extra_body
           
            # log request_kwargs (excluding sensitive fields)
            filtered_kwargs = {
                k: v for k, v in request_kwargs.items()
                if k not in ("eval_llm_list", "judge_llm_list")
            }
            LOG.info(f"OpenAI Request kwargs: {filtered_kwargs}")
            
            response = await self.aclient.chat.completions.create(**request_kwargs)
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
