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
import os
from typing import Callable, Optional

from openai import AsyncOpenAI, OpenAI

from utils.basic_utils import ValidateError


def create_llm_client(
    api_key: str, base_url: str, timeout: int = 10, is_async_client: bool = False
) -> AsyncOpenAI | OpenAI:
    if is_async_client:
        return AsyncOpenAI(
            api_key=api_key,
            base_url=base_url,
        )
    else:
        return OpenAI(
            api_key=api_key,
            base_url=base_url,
        )


async def invoke_llm_async(
    prompt: str,
    model: str,
    client: Optional[AsyncOpenAI] = None,
    *,
    enable_thinking: Optional[bool] = False,
    streaming: Optional[bool] = False,
    validator: Optional[Callable[[str], bool]] = None,
):
    client = client or AsyncOpenAI(
        api_key=os.getenv("DASHSCOPE_API_KEY"),
        base_url=os.getenv("DASHSCOPE_ENDPOINT"),
    )
    # semaphore = asyncio.Semaphore(int(os.environ.get("LLM_RATE_LIMIT", 20)))
    # async with semaphore:
    completion = await client.chat.completions.create(
        model=model,
        reasoning_effort="medium",
        # seed=random.randint(1, 1000000),
        extra_body={"enable_thinking": enable_thinking},
        stream=streaming,
        messages=[
            {
                "role": "system",
                "content": """TODO""",
            },
            {"role": "user", "content": prompt},
        ],
    )

    final_ans = ""
    if not streaming:
        final_ans = completion.choices[0].message.content
    else:
        async for event in completion:
            final_ans += (
                (event.choices[0].delta.reasoning_content if enable_thinking else None)
                or event.choices[0].delta.content
                or ""
            )

    if validator:
        if not validator(final_ans):
            raise ValidateError(
                valid=False, required=True, message=f"Validator: Invalid response from LLM."
            )

    return final_ans


def invoke_llm(
    prompt: str,
    model: str,
    client: Optional[OpenAI] = None,
    *,
    enable_thinking: Optional[bool] = False,
    streaming: Optional[bool] = False,
    validator: Optional[Callable[[str], bool]] = None,
):
    client = client or OpenAI(
        api_key=os.getenv("DASHSCOPE_API_KEY"),
        base_url=os.getenv("DASHSCOPE_ENDPOINT"),
    )
    completion = client.chat.completions.create(
        model=model,
        reasoning_effort="medium",
        # seed=random.randint(1, 1000000),
        extra_body={"enable_thinking": enable_thinking},
        stream=streaming,
        messages=[
            {
                "role": "system",
                "content": """You are a helpful assistant.""",
            },
            {"role": "user", "content": prompt},
        ],
    )

    final_ans = ""
    if not streaming:
        final_ans = completion.choices[0].message.content
    else:
        for event in completion:
            final_ans += (
                (event.choices[0].delta.reasoning_content if enable_thinking else None)
                or event.choices[0].delta.content
                or ""
            )

    if validator:
        if not validator(final_ans):
            raise ValidateError(
                valid=False, required=True, message=f"Validator: Invalid response from LLM."
            )
    return final_ans
