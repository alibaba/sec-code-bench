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

import asyncio
import json
from abc import ABC, abstractmethod
from collections import Counter
from pathlib import Path
from typing import Any

from verifier.evaluator.base import (
    EvaluatorResult,
    LanguageSupport
)
from verifier.llm.llm_base import LLMBase
from verifier.utils.logger_utils import Logger

LOG = Logger.get_logger(__name__)


class LanguageHandlerBase(ABC):
    """Base class for language-specific handlers.

    Provides common functionality and defines the interface that all
    language handlers must implement for functional and security testing.
    """

    @abstractmethod
    async def run_fun_llm_test(self, code_dir: Path, 
                               params: dict[str, str],
                               judge_llm_list: list[LLMBase],
                               language: LanguageSupport,
                               locale: str) -> EvaluatorResult:
        """
        Run functional LLM tests for the language.

        Args:
            code_dir: Path to the code directory
            **kwargs: Additional keyword arguments including:
                - params: dict[str, str] - File paths for evaluation
                - judge_llm_list: list[LLMBase] - LLM instances for judging
                - language: LanguageSupport - Programming language
                - locale: str - Locale for test criteria (default: "zh-CN")

        Returns:
            EvaluatorResult: Evaluation results from the LLM
        """
        raise NotImplementedError

    @abstractmethod
    async def run_fun_unit_test(self, code_dir: Path, **kwargs: Any) -> EvaluatorResult:
        """
        Run functional unit tests for the language.

        Args:
            code_dir: Path to the code directory
            **kwargs: Additional keyword arguments

        Returns:
            EvaluatorResult: Test results including success status and error messages
        """
        raise NotImplementedError

    @abstractmethod
    async def run_sec_llm_test(self, code_dir: Path, **kwargs: Any) -> EvaluatorResult:
        """
        Run security LLM tests for the language.

        Args:
            code_dir: Path to the code directory
            **kwargs: Additional keyword arguments including:
                - params: dict[str, str] - File paths for evaluation
                - judge_llm_list: list[LLMBase] - LLM instances for judging
                - locale: str - Locale for test criteria (default: "zh-CN")

        Returns:
            EvaluatorResult: Security evaluation results from the LLM
        """
        raise NotImplementedError

    @abstractmethod
    async def run_sec_unit_test(self, code_dir: Path, **kwargs: Any) -> EvaluatorResult:
        """
        Run security unit tests for the language.

        Args:
            code_dir: Path to the code directory
            **kwargs: Additional keyword arguments

        Returns:
            EvaluatorResult: Security test results
        """
        raise NotImplementedError

    async def multi_llm_vote(
        self, prompt: str, llm_instances: list[LLMBase]
    ) -> EvaluatorResult:
        """Get voting results from multiple LLM instances.

        This method queries multiple LLM instances with the same prompt and
        aggregates their responses using a voting mechanism to determine the
        final result.

        Args:
            prompt: Prompt to send to the LLMs
            llm_instances: List of LLM instances to query

        Returns:
            EvaluatorResult: Aggregated results from multiple LLMs based on voting.
                The result field contains the majority vote, and stdout contains
                all individual LLM responses and reasoning.
        """
        if not llm_instances:
            return EvaluatorResult(
                success=False, error_message="No judge LLM instances provided!"
            )

        # 1. Concurrently call all models
        try:
            tasks = [llm.aquery(prompt) for llm in llm_instances]
            responses = await asyncio.gather(*tasks, return_exceptions=True)
        except Exception as e:
            LOG.error(f"LLM query failed in vote {e}")
            raise

        # 2. Parse as JSON and collect results
        results = []
        reasons = []
        errors = []
        for resp, llm in zip(responses, llm_instances, strict=False):
            # Skip if response is an exception
            if isinstance(resp, BaseException):
                LOG.error(f"LLM query failed for {llm.model}: {resp}")
                errors.append(f"{llm.model}: ERROR!! Query failed: {resp}\n")
                continue

            try:
                data = json.loads(LLMBase.response_json_format(resp))
                results.append(data["result"])
                reasons.append(data["reason"])
            except Exception as e:
                LOG.error(f"JSON parsing failed: {resp} -- {e}")
                # report error
                errors.append(
                    f"{llm.model}: ERROR!! JSON parsing failed: {resp} -- {e}\n"
                )
                continue

        # 3. Vote statistics
        if not results:
            # If there are no valid results, return negative
            return EvaluatorResult(
                success=False, error_message="No judge LLM has valid responses"
            )

        counter = Counter(results)
        most_common_result, _freq = counter.most_common(1)[0]

        # report all reasons
        majority_reasons = [
            f"{llm.model}: {res}\n{r}\n"
            for r, res, llm in zip(reasons, results, llm_instances, strict=False)
        ]
        final_reason = "\n".join(majority_reasons + errors)

        LOG.debug(f"judge result: {final_reason}")
        return EvaluatorResult(
            tests=1,
            failures=most_common_result,
            stdout=final_reason,
            success=True,
        )
