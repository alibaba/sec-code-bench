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

import json
import os
from pathlib import Path
from typing import Any, Literal

import aiofiles

from sec_code_bench.utils.logger_utils import Logger

LOG = Logger.get_logger(__name__)
OpenTextMode = Literal["r", "w", "x", "a", "r+", "w+", "x+", "a+"]


def find_first_file(directory: str | Path, filename: str) -> Path | None:
    """
    Recursively find the first file with a specific name in a directory and its subdirectories.

    Args:
        directory (Union[str, Path]): Root directory to start searching from.
        filename (str): Name of the file to search for.

    Returns:
        Optional[Path]: Path object of the first found file, or None if not found.
    """
    directory = Path(directory)

    if not directory.exists():
        return None

    for root, _, files in os.walk(directory):
        for file in files:
            if file == filename:
                return Path(root) / file

    return None


def get_content(fpath: str | Path) -> str | None:
    path = Path(fpath)
    if not path.exists():
        return None
    with open(path, encoding="utf-8") as f:
        return f.read()


async def get_content_async(fpath: str | Path) -> str | None:
    """
    Asynchronously read and return the content of a file.

    Args:
        fpath (Union[str, Path]): Path to the file to read.

    Returns:
        Optional[str]: Content of the file as a string, or None
        if the file doesn't exist.
    """
    path = Path(fpath)
    if not path.exists():
        return None
    async with aiofiles.open(path, encoding="utf-8") as f:
        return await f.read()


def save_file(file_path: str | Path, content: str, overwrite: bool = False) -> None:
    path = Path(file_path)

    if not overwrite and path.exists():
        return

    path.parent.mkdir(parents=True, exist_ok=True)

    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content)


def write_file(
    file_path: str | Path,
    mode: OpenTextMode = "w",
    encoding: str = "utf-8",
    content: str = "",
) -> None:
    """
    Asynchronously write content to a file with optional mode and encoding.

    Args:
        file_path (Union[str, Path]): Path to the file to write.
        mode (OpenTextMode, optional): File opening mode. Defaults to "w".
        encoding (str, optional): File encoding. Defaults to "utf-8".
        content (str, optional): Content to write to the file. Defaults to "".

    Returns:
        None
    """
    path = Path(file_path)
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, mode, encoding=encoding) as f:
        f.write(content)


async def save_file_async(
    file_path: str | Path, content: str, overwrite: bool = False
) -> None:
    path = Path(file_path)

    if not overwrite and path.exists():
        return

    path.parent.mkdir(parents=True, exist_ok=True)

    async with aiofiles.open(file_path, "w", encoding="utf-8") as f:
        await f.write(content)


async def write_file_async(
    file_path: str | Path,
    mode: OpenTextMode = "w",
    encoding: str = "utf-8",
    content: str = "",
) -> None:
    """
    Asynchronously write content to a file with optional mode and encoding.

    Args:
        file_path (Union[str, Path]): Path to the file to write.
        mode (OpenTextMode, optional): File opening mode. Defaults to "w".
        encoding (str, optional): File encoding. Defaults to "utf-8".
        content (str, optional): Content to write to the file. Defaults to "".

    Returns:
        None
    """
    path = Path(file_path)
    path.parent.mkdir(parents=True, exist_ok=True)
    async with aiofiles.open(path, mode, encoding=encoding) as f:
        await f.write(content)


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
        case_log_result["testcase"] = {
            "name": case.case_id,
            "language": case.language.value,
            "description": (
                f"{case.FuncTester.value} for FunTest, {case.SecTester.value} for SecTest"
            ),
        }
        file_path = ""
        try:
            file_path = result_dir / f"{case.case_id}.json"
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
                if cycle not in case_log_result:
                    case_log_result[cycle] = {}

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
                        from sec_code_bench.evaluator.base import EvaluatorResult

                        sec_result = EvaluatorResult(
                            success=False,
                            error_message=f"No security result found for scenario {scenario}",
                        )
                    param_contents: dict[str, str] = {}
                    try:
                        # 安全地获取代码路径
                        if (
                            hasattr(case, "code_paths")
                            and case.code_paths
                            and cycle in case.code_paths
                            and scenario in case.code_paths[cycle]
                        ):
                            code_path = case.code_paths[cycle][scenario]
                        else:
                            LOG.warning(
                                f"No code path found for {case.case_id} cycle {cycle} scenario {scenario}"
                            )
                            code_path = None

                        for key, file_path in case.params.items():
                            try:
                                if code_path:
                                    fpath = code_path / Path(file_path)
                                    if not fpath.exists():
                                        content = f"File not found for {fpath}"
                                    else:
                                        content = get_content(fpath)
                                else:
                                    content = f"No code path available for {file_path}"
                                param_contents[key] = content
                            except Exception as e:
                                LOG.error(
                                    f"Failed to read file content {fpath if 'fpath' in locals() else file_path}: {e}"
                                )
                                param_contents[key] = f"Error reading file: {e}"
                    except Exception as e:
                        LOG.error(f"Error processing parameter contents: {e}")
                        param_contents = {
                            "error": f"Failed to process param contents: {e}"
                        }

                    # Add scenario data to cycle dictionary instead of
                    # overwriting the entire cycle
                    try:
                        case_log_result[cycle][scenario.value] = {
                            "code": param_contents,
                            "function": {
                                "result": fun_result.if_pass(),
                                "reason": "\n\n".join(
                                    filter(
                                        None,
                                        [
                                            getattr(fun_result, "stdout", ""),
                                            getattr(fun_result, "stderr", ""),
                                            getattr(fun_result, "error_message", ""),
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
                        case_log_result[cycle][scenario.value] = {
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
                case_logfile_path = result_dir / f"{case.case_id}.json"
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
