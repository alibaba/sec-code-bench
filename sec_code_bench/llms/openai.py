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


import openai
from typing_extensions import override

from .llm import (
    DEFAULT_TEMPERATURE,
    DEFAULT_TOP_P,
    LLM,
    LLMConfig,
)


class OPENAI(LLM):
    """Accessing LLM In OPENAI Format"""

    def __init__(self, config: LLMConfig) -> None:
        super().__init__(config)
        self.client = openai.OpenAI(api_key=self.api_key, base_url=self.url)  # noqa

    @override
    def query(
        self,
        prompt: str,
        temperature: float = DEFAULT_TEMPERATURE,
        top_p: float = DEFAULT_TOP_P,
    ) -> str:
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
