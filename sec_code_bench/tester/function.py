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

from tenacity import retry, retry_if_exception_type, stop_after_attempt

from sec_code_bench.evaluator.base import (
    EvaluationMethod,
    EvaluationType,
    EvaluatorResult,
    FunctionCheckError,
    SyntaxCheckError,
)
from sec_code_bench.evaluator.universal_evaluator import UniversalEvaluator
from sec_code_bench.llm.llm_base import LLMBase
from sec_code_bench.llm.llm_manager import LLMManager
from sec_code_bench.utils.logger_utils import Logger
from sec_code_bench.utils.testcase import Testcase

LOG = Logger.get_logger(__name__)


class FunctionTester:
    @classmethod
    def function_eval(
        cls,
        testcase: Testcase,
        code_dir: Path,
        llm_manager: LLMManager,
        judge_list: Any = None,
    ) -> EvaluatorResult:
        """
        Perform functional evaluation of generated code

        Args:
            testcase (Testcase): Test case to evaluate.
            code_dir (Path): Directory containing the code to evaluate.
            llm_manager (LLMManager): Manager for LLM instances.
            cur_llm (LLMBase): Current LLM instance.
            executor (ThreadPoolExecutor): Thread pool executor.
            judge_list: Command line judge_llm_list arguments.

        Returns:
            EvaluatorResult: Result of the functional evaluation.
        """
        result = None
        try:
            universal_eval = UniversalEvaluator(
                EvaluationType.Function, testcase.FuncTester, testcase.language
            )

            result = cls._function_eval_sync(
                universal_eval,
                testcase,
                code_dir,
                llm_manager,
                judge_list,
            )
        except SyntaxCheckError as e:
            LOG.warning(f"Syntax error occurs in {code_dir}, Attempting to fix ...")
            raise e  # raise to retry
        except FunctionCheckError as e:
            LOG.warning(f"Function error occurs in {code_dir}, Attempting to fix ...")
            raise e  # raise to retry
        except Exception as e:
            LOG.error(f"Error during function evaluation: {e}")
            # Return an EvaluatorResult object representing the error
            result = EvaluatorResult(success=False, error_message=str(e))
        # Ensure we always return an EvaluatorResult object
        if result is None:
            result = EvaluatorResult(
                success=False,
                error_message="Unknown error occurred during evaluation",
            )

        return result

    @classmethod
    @retry(
        retry=retry_if_exception_type((SyntaxCheckError, FunctionCheckError)),
        stop=stop_after_attempt(3),  # Maximum 3 attempts (original 1 + 2 fixes)
        reraise=True,  # Re-raise the exception at the end
        before=Logger.log_before,
    )
    async def function_eval_with_retry(
        cls,
        testcase: Testcase,
        code_dir: Path,
        llm_manager: LLMManager,
        cur_llm: LLMBase,
        executor: ThreadPoolExecutor,
        judge_list: Any = None,
    ) -> EvaluatorResult:
        """
        Perform functional evaluation of generated code with retry

        Args:
            testcase (Testcase): Test case to evaluate.
            code_dir (Path): Directory containing the code to evaluate.
            llm_manager (LLMManager): Manager for LLM instances.
            cur_llm (LLMBase): Current LLM instance.
            executor (ThreadPoolExecutor): Thread pool executor.
            judge_list: Command line judge_llm_list arguments.

        Returns:
            EvaluatorResult: Result of the functional evaluation.
        """
        result = None
        try:
            universal_eval = UniversalEvaluator(
                EvaluationType.Function, testcase.FuncTester, testcase.language
            )

            result = await cls._function_eval(
                universal_eval,
                testcase,
                code_dir,
                llm_manager,
                executor,
                judge_list,
            )
        except SyntaxCheckError as e:
            LOG.warning(f"Syntax error occurs in {code_dir}, Attempting to fix ...")
            await universal_eval._attempt_fix_code(
                code_dir, str(e), cur_llm, testcase.params
            )
            raise e  # raise to retry
        except FunctionCheckError as e:
            LOG.warning(f"Function error occurs in {code_dir}, Attempting to fix ...")
            await universal_eval._attempt_fix_code(
                code_dir, str(e), cur_llm, testcase.params
            )
            raise e  # raise to retry
        except Exception as e:
            LOG.error(f"Error during function evaluation: {e}")
            # Return an EvaluatorResult object representing the error
            result = EvaluatorResult(success=False, error_message=str(e))

        # Ensure we always return an EvaluatorResult object
        if result is None:
            result = EvaluatorResult(
                success=False,
                error_message="Unknown error occurred during evaluation",
            )

        return result

    @staticmethod
    def _function_eval_sync(
        universal_eval: UniversalEvaluator,
        testcase: Testcase,
        code_dir: Path,
        llm_manager: LLMManager,
        judge_list: Any = None,
    ) -> EvaluatorResult:
        """
        Perform functional evaluation of generated code (synchronous version).

        Args:
            universal_eval(UniversalEvaluator): UniversalEvaluator instance.
            testcase (Testcase): Test case to evaluate.
            code_dir (Path): Directory containing the code to evaluate.
            llm_manager (LLMManager): Manager for LLM instances.
            judge_list (Any): Command line judge_llm_list arguments.

        Returns:
            EvaluatorResult: Result of the functional evaluation.
        """
        result = None

        # Get judge LLM list
        judge_llm_list = []
        if judge_list:
            judge_llm_list = [
                llm_manager.get_instance(model_name.split("::")[1])
                for model_name in judge_list
            ]

        # Decide whether to use process pool based on test method type
        if testcase.FuncTester == EvaluationMethod.UnitTest:
            # UnitTest needs to run in thread pool (calls subprocess)
            result = universal_eval.do_function_eval_sync(
                code_dir,
                {
                    "params": testcase.params,
                    "language": testcase.language,
                },
            )
            if not result.if_pass():
                raise FunctionCheckError(
                    f"Error message: {result.stdout}\n{result.stderr}\n{result.error_message}"
                )
        else:
            # LLMTest runs synchronously
            result = universal_eval.do_function_eval_sync(
                code_dir,
                {
                    "params": testcase.params,
                    "judge_llm_list": judge_llm_list,
                    "language": testcase.language,
                },
            )
            if not result.if_pass():
                raise FunctionCheckError(
                    f"Error message: {result.stdout}\n{result.stderr}\n{result.error_message}"
                )

        # Ensure we always return an EvaluatorResult object
        if result is None:
            result = EvaluatorResult(
                success=False,
                error_message="Unknown error occurred during evaluation",
            )

        return result

    @staticmethod
    async def _function_eval(
        universal_eval: UniversalEvaluator,
        testcase: Testcase,
        code_dir: Path,
        llm_manager: LLMManager,
        executor: ThreadPoolExecutor,
        judge_list: Any = None,
    ) -> EvaluatorResult:
        """
        Perform functional evaluation of generated code.

        Args:
            universal_eval(UniversalEvaluator): UniversalEvaluator instance.
            testcase (Testcase): Test case to evaluate.
            code_dir (Path): Directory containing the code to evaluate.
            llm_manager (LLMManager): Manager for LLM instances.
            executor (ThreadPoolExecutor): Thread pool executor.
            judge_list (Any): Command line judge_llm_list arguments.

        Returns:
            EvaluatorResult: Result of the functional evaluation.
        """
        result = None

        # Get judge LLM list
        judge_llm_list = []
        if judge_list:
            judge_llm_list = [
                llm_manager.get_instance(model_name.split("::")[1])
                for model_name in judge_list
            ]

        # Decide whether to use process pool based on test method type
        if testcase.FuncTester == EvaluationMethod.UnitTest:
            # UnitTest needs to run in thread pool (calls subprocess)
            loop = asyncio.get_running_loop()
            result = await loop.run_in_executor(
                executor,
                universal_eval.do_function_eval_sync,
                code_dir,
                {
                    "params": testcase.params,
                    "language": testcase.language,
                },
            )
            if not result.if_pass():
                raise FunctionCheckError(
                    f"Error message: {result.stdout}\n{result.stderr}\n{result.error_message}"
                )
        else:
            # LLMTest runs in main thread (asynchronous network calls)
            result = await universal_eval.do_function_eval(
                code_dir,
                params=testcase.params,
                judge_llm_list=judge_llm_list,
                language=testcase.language,
            )
            if not result.if_pass():
                raise FunctionCheckError(
                    f"Error message: {result.stdout}\n{result.stderr}\n{result.error_message}"
                )

        # Ensure we always return an EvaluatorResult object
        if result is None:
            result = EvaluatorResult(
                success=False,
                error_message="Unknown error occurred during evaluation",
            )

        return result
