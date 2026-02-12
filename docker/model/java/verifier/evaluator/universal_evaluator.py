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
from pathlib import Path
from typing import Any

from verifier.evaluator.base import (
    EvaluationMethod,
    EvaluationType,
    EvaluatorBase,
    EvaluatorResult,
    LanguageSupport,
    SyntaxCheckError,
)
from verifier.evaluator.registry import HandlerFactory
from verifier.utils.logger_utils import Logger

LOG = Logger.get_logger(__name__)


class UniversalEvaluator(EvaluatorBase):
    """Universal evaluator for code assessment.

    Supports both functional and security evaluation through different testing methods.
    """

    def __init__(
        self,
        eval_type: EvaluationType,
        tester_type: EvaluationMethod,
        language: LanguageSupport,
    ) -> None:
        """Initialize the universal evaluator.

        Args:
            eval_type: The type of evaluation to perform
            tester_type: The type of tester to use
            language: The programming language being evaluated
        """
        super().__init__(eval_type, tester_type, language)
        self.handler = HandlerFactory.get_handler(language)
        # Automatically establish mapping from "test method" to handler methods
        self.function_method_map = {
            EvaluationMethod.LLMTest: self.handler.run_fun_llm_test,
            EvaluationMethod.UnitTest: self.handler.run_fun_unit_test,
            # Add new fun-tester here only
        }
        self.security_method_map = {
            EvaluationMethod.LLMTest: self.handler.run_sec_llm_test,
            EvaluationMethod.UnitTest: self.handler.run_sec_unit_test,
            # Add new sec-tester here only
        }

    async def do_function_eval(self, code_dir: Path, **kwargs: Any) -> EvaluatorResult:
        """Perform functional evaluation on the code.

        Args:
            code_dir: Directory containing the code to evaluate
            **kwargs: Additional arguments for the evaluation

        Returns:
            Result of the functional evaluation

        Raises:
            SyntaxCheckError: If there are syntax errors in the code
        """
        functional_test_result = None
        try:
            fun_eval = self.function_method_map.get(self.tester_type)
            if not fun_eval:
                raise ValueError(f"Invalid function tester type: {self.tester_type}")
            functional_test_result = await fun_eval(code_dir, **kwargs)
        except SyntaxCheckError:
            raise  # Syntax error, raise directly for retry
        except Exception as e:
            LOG.error(f"Function eval Error: {e}", exc_info=True)
            if not isinstance(functional_test_result, EvaluatorResult):
                functional_test_result = EvaluatorResult(
                    success=False, error_message=str(e)
                )
        return functional_test_result

    async def do_security_eval(self, code_dir: Path, **kwargs: Any) -> EvaluatorResult:
        """Perform security evaluation on the code.

        Args:
            code_dir: Directory containing the code to evaluate
            **kwargs: Additional arguments for the evaluation

        Returns:
            Result of the security evaluation
        """
        security_test_result = None
        try:
            sec_eval = self.security_method_map.get(self.tester_type)
            if not sec_eval:
                raise ValueError(f"Invalid security tester type: {self.tester_type}")
            security_test_result = await sec_eval(code_dir, **kwargs)
        except Exception as e:
            LOG.error(f"Security eval Error: {e}")
            if not isinstance(security_test_result, EvaluatorResult):
                security_test_result = EvaluatorResult(
                    success=False, error_message=str(e)
                )
        return security_test_result

    def do_function_eval_sync(
        self, code_dir: Path, kwargs_dict: dict[str, Any] | None = None
    ) -> EvaluatorResult:
        """Synchronously perform functional evaluation on the code.

        Args:
            code_dir: Directory containing the code to evaluate
            kwargs_dict: Additional arguments for the evaluation

        Returns:
            Result of the functional evaluation
        """
        # Use asyncio.run() which properly manages the event loop lifecycle
        try:
            kwargs = kwargs_dict or {}
            result = asyncio.run(self.do_function_eval(code_dir, **kwargs))
            return result
        except SyntaxCheckError:
            raise  # Syntax error, raise directly for retry
        except Exception as e:
            LOG.error(f"Error during function evaluation: {e}")
            return EvaluatorResult(
                success=False,
                error_message=f"Error during function evaluation: {e}",
            )

    def do_security_eval_sync(
        self, code_dir: Path, kwargs_dict: dict[str, Any] | None = None
    ) -> EvaluatorResult:
        """Synchronously perform security evaluation on the code.

        Args:
            code_dir: Directory containing the code to evaluate
            kwargs_dict: Additional arguments for the evaluation

        Returns:
            Result of the security evaluation
        """
        # Use asyncio.run() which properly manages the event loop lifecycle
        try:
            kwargs = kwargs_dict or {}
            result = asyncio.run(self.do_security_eval(code_dir, **kwargs))
            return result
        except Exception as e:
            LOG.error(f"Error during security evaluation: {e}")
            return EvaluatorResult(
                success=False,
                error_message=f"Error during security evaluation: {e}",
            )

