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
from pathlib import Path
from typing import Any

from sec_code_bench.evaluator.base import (
    EvaluationMethod,
    EvaluationType,
    EvaluatorBase,
    EvaluatorResult,
    LanguageSupport,
    SyntaxCheckError,
)
from sec_code_bench.evaluator.registry import HandlerFactory
from sec_code_bench.llm.llm_base import LLMBase
from sec_code_bench.utils.fdisk_utils import get_content_async, write_file_async
from sec_code_bench.utils.logger_utils import Logger

LOG = Logger.get_logger(__name__)

PROMPT_FixSyntaxError = """
你是一个专业的程序员，你的任务是修复以下代码中的语法语义和功能错误。

错误信息如下：
{error_message}

出错的代码如下：
----------------------
{files}
----------------------

请分析错误并提供修复后的完整代码。只返回修复后的代码，不要添加任何解释。
请提供修复后完整的代码，按照以下JSON格式返回:
{{
    "file_path1": "修复后的完整文件内容1",
    "file_path2": "修复后的完整文件内容2"
}}

重要：只返回有效的JSON，不要添加其他文本。请确保输出是严格合法 JSON，无 Markdown 代码块，无多余反斜杠或遗漏引号。
"""


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

    async def _attempt_fix_code(
        self,
        code_dir: Path,
        err_msg: str,
        llm: LLMBase,
        params: dict[str, str],
    ) -> None:
        """Attempt to fix code using LLM based on error message.

        Args:
            code_dir: Directory containing the code to fix
            err_msg: Error message describing the issue
            llm: LLM instance to use for code fixing
            params: Dictionary mapping parameter names to file paths
        """
        param_contents: dict[str, str] = {}
        for key, file_path in params.items():
            fpath = code_dir / Path(file_path)
            if not fpath.exists():
                raise FileNotFoundError(f"File not found for param '{key}': {fpath}")
            content = await get_content_async(fpath)
            param_contents[key] = content

        param_contents_str = json.dumps(param_contents, ensure_ascii=False, indent=2)

        prompt_template = PROMPT_FixSyntaxError.strip()
        final_prompt = prompt_template.format(
            error_message=err_msg, files=param_contents_str
        )
        # LOG.debug("Syntax/semantic fix prompt for LLM:\n" + final_prompt)
        try:
            response = await llm.aquery(final_prompt)
        except Exception as e:
            LOG.error(f"Error query LLM in _attempt_fix_code: {e}")
            raise e

        # Get code after fix syntax error
        try:
            new_files: dict[str, str] = json.loads(LLMBase.response_json_format(response))
        except Exception as e:
            LOG.error(f"JSON error: {e}, response content: {response}")
            raise SyntaxCheckError(
                f"JSON error: {e}, response content: {response}"
            ) from e

        # Write to files
        for file, content in new_files.items():
            if file not in params:
                LOG.warning(f"{file} not in params, skipping")
                continue
            file_path = code_dir / params[file]
            try:
                await write_file_async(file_path, "w", "utf-8", content)
            except Exception as e:
                LOG.error(f"Failed to write file {file_path}: {e}", exc_info=True)
                continue
