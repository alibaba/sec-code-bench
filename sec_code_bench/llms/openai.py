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


import openai, time
import asyncio
from typing_extensions import override
from utils.rate_limiter import AsyncRateLimiter
from tenacity import retry, stop_after_attempt, wait_fixed, retry_if_exception_type
from .llm import (
    DEFAULT_TEMPERATURE,
    DEFAULT_TOP_P,
    LLM,
    LLMConfig,
)

import logging
LOG: logging.Logger = logging.getLogger(__name__)


class OPENAI(LLM):
    """Accessing LLM In OPENAI Format"""

    def __init__(self, config: LLMConfig, rate_limiter: AsyncRateLimiter) -> None:
        super().__init__(config)
        self.client = openai.OpenAI(api_key=self.api_key, base_url=self.url)  # noqa
        self.aclient = openai.AsyncOpenAI(
            api_key=self.api_key,
            base_url=self.url,
            max_retries=0,
        )
        self.rate_limiter = rate_limiter
    @override
    def query(
        self,
        prompt: str,
        temperature: float = DEFAULT_TEMPERATURE,
        top_p: float = DEFAULT_TOP_P,
    ) -> str:
        for attempt in range(8):
            try:
                response = self.client.chat.completions.create(
                    model=self.model,
                    stream=False,
                    messages=[
                        {
                            "role": "system",
                            "content": """TODO""",
                        },
                        {"role": "user", "content": prompt},
                    ],
                    temperature=(temperature),
                    top_p=top_p,
                )
                return response.choices[0].message.content
            except openai.RateLimitError as e:
                LOG.error(e)
                retry_after = e.response.headers.get("retry-after")
                if retry_after:
                    sleep_sec = float(retry_after)
                else:
                    sleep_sec = 2 ** attempt
                time.sleep(sleep_sec)
            except Exception as e:
                LOG.error(e)
                raise
        raise RuntimeError("Too many requests")

    @retry(
        retry=retry_if_exception_type((openai.RateLimitError, openai.APIError, openai.Timeout)),
        stop=stop_after_attempt(3),
        wait=wait_fixed(3),
        reraise=True
    )
    @override
    async def aquery(
        self,
        prompt: str,
        temperature: float = DEFAULT_TEMPERATURE,
        top_p: float = DEFAULT_TOP_P,
    ) -> str:
            try:
                async with self.rate_limiter:
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
                        temperature=(temperature),
                        top_p=top_p,
                    )
                    return response.choices[0].message.content
            except openai.RateLimitError as e:
                LOG.error(f"openai.RateLimitError : {e}")
                retry_after = e.response.headers.get("retry-after")
                if retry_after:
                    sleep_sec = float(retry_after)
                    LOG.warning(f"Rate limit hit, using retry-after: {sleep_sec} seconds")
                    await asyncio.sleep(sleep_sec)
                else:
                    await asyncio.sleep(10)
                raise
            except Exception as e:
                LOG.error(f"Unexpected error during LLM query: {str(e)}")
                raise
    
    async def aclose(self):
        await self.aclient.close()
