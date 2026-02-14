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

"""Testcase persistence and storage operations."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from sec_code_bench.evaluator.base import EvaluatorResult
from sec_code_bench.utils.fdisk_utils import save_file
from sec_code_bench.utils.logger_utils import Logger
from sec_code_bench.utils.testcase import Testcase

LOG = Logger.get_logger(__name__)

# save test results to disk
def save_test_results(result_dir: Path, testcases: list[Testcase]) -> None:
    """
    Record log directory with test results.

    Args:
        result_dir (Path): Directory to store result files.
        testcases (list[Testcase]): List of test cases to record.
    """
    for case in testcases:
        if len(case.FunResults) == 0 or len(case.SecResults) == 0:
            LOG.error(
                f"No results for {case.case_id}: {case.FunResults} {case.SecResults}"
            )
            assert len(case.FunResults) == len(case.SecResults)
        case_log_result: dict[str, Any] = {}
        case_log_result["score"] = {}
        # Add test case information
        if case.FuncTester and case.SecTester:
            description = (
                f"{case.FuncTester.value} for FunTest, {case.SecTester.value} for SecTest"
            )
        else:
            description = "Remote verification mode"
        case_log_result["testcase"] = {
            "name": case.case_id,
            "language": case.language.value,
            "description": description,
        }
        file_path = ""
        try:
            # Create language subdirectory for organized results
            lang_dir = result_dir / case.language.value
            lang_dir.mkdir(parents=True, exist_ok=True)
            # Replace '/' with '_' to flatten nested testcase IDs into a single filename
            safe_case_id = case.case_id.replace("/", "_")
            file_path = lang_dir / f"{safe_case_id}.json"
            save_file(
                file_path,
                json.dumps(case_log_result, indent=2, ensure_ascii=False),
                True,
            )
        except Exception as e:
            LOG.error(f"Failed to write result log {file_path}: {e}")

        # Add test results
        try:
            # 检查是否有结果数据
            if not hasattr(case, "FunResults") or not case.FunResults:
                LOG.warning(f"No function results found for case {case.case_id}")
                return

            # 调试信息：打印结果结构
            LOG.debug(f"FunResults for {case.case_id}: {case.FunResults}")
            if hasattr(case, "SecResults"):
                LOG.debug(f"SecResults for {case.case_id}: {case.SecResults}")
            else:
                LOG.warning(f"No SecResults attribute for case {case.case_id}")

            for cycle, scenario_results in case.FunResults.items():
                cycle_key = str(cycle)
                if cycle_key not in case_log_result:
                    case_log_result[cycle_key] = {}

                for scenario, _ in scenario_results.items():
                    fun_result = case.FunResults[cycle][scenario]

                    # 安全地获取安全测试结果，如果不存在则创建默认结果
                    if (
                        hasattr(case, "SecResults")
                        and case.SecResults
                        and cycle in case.SecResults
                        and scenario in case.SecResults[cycle]
                    ):
                        sec_result = case.SecResults[cycle][scenario]
                    else:
                        LOG.warning(
                            f"No security result found for {case.case_id} cycle {cycle} scenario {scenario}"
                        )
                        sec_result = EvaluatorResult(
                            success=False,
                            error_message=f"No security result found for scenario {scenario}",
                        )
                    param_contents: dict[str, str] = {}
                    try:
                        # Check for generated_code first (for remote mode)
                        if (
                            hasattr(case, "generated_code")
                            and case.generated_code
                            and cycle in case.generated_code
                            and scenario in case.generated_code[cycle]
                        ):
                            # For remote mode, store the raw LLM response
                            param_contents["llm_response"] = case.generated_code[cycle][scenario]
                        else:
                            LOG.warning(
                                f"No code found for {case.case_id} cycle {cycle} scenario {scenario}"
                            )
                    except Exception as e:
                        LOG.error(f"Error processing parameter contents: {e}")
                        param_contents = {
                            "error": f"Failed to process param contents: {e}"
                        }

                    # Add scenario data to cycle dictionary instead of
                    # overwriting the entire cycle
                    try:
                        fun_stdout = getattr(fun_result, "stdout", "")
                        fun_stderr = getattr(fun_result, "stderr", "")
                        fun_error_msg = getattr(fun_result, "error_message", "")

                        case_log_result[cycle_key][scenario.value] = {
                            "code": param_contents,
                            "function": {
                                "result": fun_result.if_pass(),
                                "reason": "\n\n".join(
                                    filter(
                                        None,
                                        [
                                            fun_stdout,
                                            fun_stderr,
                                            fun_error_msg,
                                        ],
                                    )
                                ),
                            },
                            "security": {
                                "result": sec_result.if_pass(),
                                "reason": "\n\n".join(
                                    filter(
                                        None,
                                        [
                                            getattr(sec_result, "stdout", ""),
                                            getattr(sec_result, "stderr", ""),
                                            getattr(sec_result, "error_message", ""),
                                        ],
                                    )
                                ),
                            },
                        }
                    except Exception as e:
                        LOG.error(
                            f"Error building result scenario={scenario}, "
                            f"cycle={cycle}: {e}"
                        )
                        case_log_result[cycle_key][scenario.value] = {
                            "error": f"Failed to build result data: {e}"
                        }
                    # 安全地设置分数，避免 KeyError
                    if hasattr(case, "score") and case.score and scenario in case.score:
                        case_log_result["score"][scenario.value] = case.score[scenario]
                    else:
                        LOG.warning(
                            f"No score found for scenario {scenario} in case {case.case_id}"
                        )
                        case_log_result["score"][scenario.value] = 0.0

            case_logfile_path = ""
            try:
                # Create language subdirectory for organized results
                lang_dir = result_dir / case.language.value
                lang_dir.mkdir(parents=True, exist_ok=True)
                # Replace '/' with '_' to flatten nested testcase IDs into a single filename
                safe_case_id = case.case_id.replace("/", "_")
                case_logfile_path = lang_dir / f"{safe_case_id}.json"
                save_file(
                    case_logfile_path,
                    json.dumps(case_log_result, indent=2, ensure_ascii=False),
                    True,
                )
            except Exception as e:
                LOG.error(f"Failed to write result log {case_logfile_path}: {e}")
        except Exception as e:
            LOG.error(f"Error recording test case result {case.case_id}: {e}")
            import traceback

            LOG.error(f"Traceback: {traceback.format_exc()}")
            continue
