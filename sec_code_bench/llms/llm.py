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

import logging
from abc import ABC, abstractmethod
from dataclasses import dataclass

LOG: logging.Logger = logging.getLogger(__name__)

DEFAULT_MAX_TOKENS = 2048
DEFAULT_TEMPERATURE = 0.6
DEFAULT_TOP_P = 0.9


@dataclass
class LLMConfig:
    model: str
    url: str
    api_key: str | None = None


class LLM(ABC):
    def __init__(
        self,
        config: LLMConfig,
    ) -> None:
        self.model: str = config.model
        self.url = config.url
        self.api_key: str | None = config.api_key

    @abstractmethod
    def query(
        self,
        prompt: str,
        temperature: float = DEFAULT_TEMPERATURE,
        top_p: float = DEFAULT_TOP_P,
    ) -> str:
        pass

    @abstractmethod
    async def aquery(
        self,
        prompt: str,
        temperature: float = DEFAULT_TEMPERATURE,
        top_p: float = DEFAULT_TOP_P,
    ) -> str:
        pass