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
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from typing import Any

from sec_code_bench.evaluator.base import (
    EvaluationMethod,
    EvaluationType,
    EvaluatorResult,
)
from sec_code_bench.evaluator.universal_evaluator import UniversalEvaluator
from sec_code_bench.llm.llm_manager import LLMManager
from sec_code_bench.utils.logger_utils import Logger
from sec_code_bench.utils.testcase import Testcase

LOG = Logger.get_logger(__name__)


class SecurityTester:
    @classmethod
    def security_eval_sync(
        cls,
        testcase: Testcase,
        code_dir: Path,
        llm_manager: LLMManager,
        judge_list: Any = None,
    ) -> EvaluatorResult:
        """
        Perform security evaluation of generated code (synchronous version).

        Args:
            testcase (Testcase): Test case to evaluate.
            code_dir (Path): Directory containing the code to evaluate.
            llm_manager (LLMManager): Manager for LLM instances.
            judge_list: Command line judge_llm_list arguments.

        Returns:
            EvaluatorResult: Result of the security evaluation.
        """
        result = None
        try:
            universal_eval = UniversalEvaluator(
                EvaluationType.Security, testcase.SecTester, testcase.language
            )

            # Get judge LLM list
            judge_llm_list = []
            if judge_list:
                judge_llm_list = [
                    llm_manager.get_instance(model_name.split("::")[1])
                    for model_name in judge_list
                ]

            # Decide whether to use thread pool based on test method type
            if testcase.SecTester == EvaluationMethod.UnitTest:
                result = universal_eval.do_security_eval_sync(
                    code_dir,
                    testcase.params,
                )
            else:
                # LLMTest runs synchronously
                result = universal_eval.do_security_eval_sync(
                    code_dir,
                    {
                        "params": testcase.params,
                        "judge_llm_list": judge_llm_list,
                        "language": testcase.language,
                    },
                )
        except Exception as e:
            LOG.error(f"Error during security evaluation: {e}")
            # Return an EvaluatorResult object representing the error
            result = EvaluatorResult(success=False, error_message=str(e))

        # Ensure we always return an EvaluatorResult object
        if result is None:
            result = EvaluatorResult(
                success=False,
                error_message="Unknown error occurred during security evaluation",
            )

        return result

    @classmethod
    async def security_eval(
        cls,
        testcase: Testcase,
        code_dir: Path,
        llm_manager: LLMManager,
        executor: ThreadPoolExecutor,
        judge_list: Any = None,
    ) -> EvaluatorResult:
        """
        Perform security evaluation of generated code.

        Args:
            testcase (Testcase): Test case to evaluate.
            code_dir (Path): Directory containing the code to evaluate.
            llm_manager (LLMManager): Manager for LLM instances.
            executor (ThreadPoolExecutor): Thread pool executor.
            judge_list: Command line judge_llm_list arguments.

        Returns:
            EvaluatorResult: Result of the security evaluation.
        """
        result = None
        try:
            universal_eval = UniversalEvaluator(
                EvaluationType.Security, testcase.SecTester, testcase.language
            )

            # Get judge LLM list
            judge_llm_list = []
            if judge_list:
                judge_llm_list = [
                    llm_manager.get_instance(model_name.split("::")[1])
                    for model_name in judge_list
                ]

            # Decide whether to use thread pool based on test method type
            if testcase.SecTester == EvaluationMethod.UnitTest:
                loop = asyncio.get_running_loop()
                result = await loop.run_in_executor(
                    executor,
                    universal_eval.do_security_eval_sync,
                    code_dir,
                    testcase.params,
                )
            else:
                # LLMTest runs in main thread (asynchronous network calls)
                result = await universal_eval.do_security_eval(
                    code_dir,
                    params=testcase.params,
                    judge_llm_list=judge_llm_list,
                    language=testcase.language,
                )
        except Exception as e:
            LOG.error(f"Error during security evaluation: {e}")
            # Return an EvaluatorResult object representing the error
            result = EvaluatorResult(success=False, error_message=str(e))

        # Ensure we always return an EvaluatorResult object
        if result is None:
            result = EvaluatorResult(
                success=False,
                error_message="Unknown error occurred during security evaluation",
            )

        return result
