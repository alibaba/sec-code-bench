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

from pathlib import Path
from typing import Any

from sec_code_bench.utils.logger_utils import Logger

LOG = Logger.get_logger(__name__)


class LanguageHandlerBase:
    """Base class for language-specific handlers."""

    async def run_fun_llm_test(self, code_dir: Path, **kwargs: Any) -> None:
        """
        Run functional LLM tests for the language.

        Args:
            code_dir: Path to the code directory
            **kwargs: Additional keyword arguments
        """
        raise NotImplementedError

    async def run_fun_unit_test(self, code_dir: Path, **kwargs: Any) -> None:
        """
        Run functional unit tests for the language.

        Args:
            code_dir: Path to the code directory
            **kwargs: Additional keyword arguments
        """
        raise NotImplementedError

    async def run_sec_llm_test(self, code_dir: Path, **kwargs: Any) -> None:
        """
        Run security LLM tests for the language.

        Args:
            code_dir: Path to the code directory
            **kwargs: Additional keyword arguments
        """
        raise NotImplementedError

    async def run_sec_unit_test(self, code_dir: Path, **kwargs: Any) -> None:
        """
        Run security unit tests for the language.

        Args:
            code_dir: Path to the code directory
            **kwargs: Additional keyword arguments
        """
        raise NotImplementedError
