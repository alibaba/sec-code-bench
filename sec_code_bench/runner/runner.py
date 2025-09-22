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

import copy
import os
import shutil
from pathlib import Path
from typing import Any

from sec_code_bench.editor import Editor
from sec_code_bench.evaluator.base import EvaluatorResult, SyntaxCheckError
from sec_code_bench.llm.llm_manager import LLMManager
from sec_code_bench.tester.function import FunctionTester
from sec_code_bench.tester.security import SecurityTester
from sec_code_bench.utils.logger_utils import Logger
from sec_code_bench.utils.testcase import Testcase, TestScenario

LOG = Logger.get_logger(__name__)

EXTEND_PROMPT = """
## 输出格式

文件名称和对应的路径为
{file_path}

请直接提供完整的Java类代码，包含必要的 import 语句和完整的类定义，并输出代码到对应文件中，注意换行的编码转义问题。
"""


class Runner:
    """Runner class for executing test cases."""

    def __init__(
        self,
        testcase: Testcase,
        scenario: TestScenario,
        editor: Editor,
        cycle: int,
        prepare: bool = False,
    ) -> None:
        """
        Initialize the Runner instance.

        Args:
            testcase (Testcase): Test case to execute.
            scenario (TestScenario): Scenario to execute.
            editor (Editor): Editor to execute test cases.
            cycle (int): Cycle to execute test cases.
            prepare (bool, optional): Whether to prepare test cases.
        """
        self.testcase = testcase
        self.scenario = scenario
        self.cycle = cycle
        self.editor = editor
        self.prepare = prepare

    # Ignore the src/test directory to avoid knowing the answer in advance
    @staticmethod
    def ignore_src_test(src: str, names: list[str]) -> set[str]:
        """
        Ignore src/test directory during copy to avoid knowing the answer in advance.

        Args:
            src: Source directory path
            names: Names in the source directory

        Returns:
            Set of directory names to ignore
        """
        if os.path.basename(src) == "src" and "test" in names:
            return {"test"}
        return set()

    def run(
        self,
        work_dir: Path,
        llm_manager: LLMManager,
        judge_llm_list: Any,
    ) -> None:
        """
        Run the test case.

        Args:
            work_dir: Working directory path
            llm_manager: LLM manager
            judge_llm_list: Judge LLM list

        Returns:
            Test results as dictionary
        """
        # Set up code template directory and temporary directory,
        # ensure successful file output
        current_dir = Path(__file__).parent.absolute()
        code_template_dir = (
            current_dir.parent.parent
            / "datasets/templates"
            / self.testcase.language.value
            / self.testcase.template
        )

        code_dir = (
            work_dir / f"{self.testcase.prompt}_{self.scenario.value}_cycle-{self.cycle}"
        )
        self.testcase.set_code_paths(self.cycle, self.scenario, code_dir)

        shutil.copytree(
            str(code_template_dir), str(code_dir), ignore=self.ignore_src_test
        )

        prompt = self.testcase.get_scenario_prompt(self.scenario)
        LOG.info(
            f"Starting testcase: {self.testcase.case_id} - {self.scenario} - {self.cycle}"
        )

        prompt = prompt.split("## 输出格式")[0] + EXTEND_PROMPT.format(
            file_path=self.testcase.params
        )

        self.editor.coding(str(code_dir), prompt, self.prepare)

        # copy testcase to ide path
        try:
            source_testcase_dir = Path(f"{code_template_dir}/src/test")
            target_test_dir = Path(f"{code_dir}/src/test")
            if not source_testcase_dir.exists():
                LOG.warning(f"source test dir not exits: {source_testcase_dir}")
                return

            if source_testcase_dir.exists():
                LOG.info(
                    f"now running copy file from {source_testcase_dir} "
                    f"to {target_test_dir}"
                )

                target_test_dir.mkdir(exist_ok=True)

                for root, _dirs, files in os.walk(source_testcase_dir):
                    root_path = Path(root)
                    rel_path = root_path.relative_to(source_testcase_dir)
                    target_dir = (
                        target_test_dir / rel_path
                        if rel_path != Path(".")
                        else target_test_dir
                    )

                    target_dir.mkdir(exist_ok=True)

                    # Ensure test case functional and security tests exist
                    for file in files:
                        source_file = root_path / file
                        target_file = target_dir / file
                        shutil.copy2(source_file, target_file)
                        LOG.info(f"copied: {target_file}")

                LOG.info("copied file success")
            else:
                LOG.warning(f"copied dir not exits: {source_testcase_dir}")

        except Exception as e:
            LOG.error(f"copied file error: {str(e)}")
            import traceback

            LOG.error(traceback.format_exc())

        try:
            fun_result = FunctionTester.function_eval(
                self.testcase, code_dir, llm_manager, judge_llm_list
            )
            short_result = copy.deepcopy(fun_result)
            short_result.stdout = "see log for details" if short_result.stdout else ""
            short_result.stderr = "see log for details" if short_result.stderr else ""
            LOG.info(
                f"Function evaluation result for {self.testcase.case_id}:"
                f"{self.scenario.value} is {short_result}"
            )
        except SyntaxCheckError as e:
            LOG.error(f"Syntax error in {code_dir}: {str(e)}")
            import traceback

            LOG.error(f"Syntax error traceback: {traceback.format_exc()}")
            self.testcase.set_error_result(
                self.cycle,
                self.scenario,
                f"Syntax error in {code_dir} \n {str(e)}",
            )
            return
        except Exception as e:
            LOG.error(f"Error running function test: {str(e)}")
            self.testcase.set_error_result(
                self.cycle,
                self.scenario,
                f"Functional check failed; security check was not performed.\n"
                f"Function test error message: {str(e)}",
            )
            return

        if not isinstance(fun_result, EvaluatorResult):
            LOG.error("Error: function test result is not an EvaluatorResult object")
            self.testcase.set_error_result(
                self.cycle,
                self.scenario,
                "Error: function test result is not an EvaluatorResult object",
            )
            return

        self.testcase.set_fun_results(self.cycle, self.scenario, fun_result)

        # not pass function test
        if not fun_result.success or not fun_result.if_pass():
            self.testcase.set_sec_results(
                self.cycle,
                self.scenario,
                EvaluatorResult(success=False, error_message="Function test failed"),
            )
            return

        try:
            sec_result = SecurityTester.security_eval_sync(
                self.testcase, code_dir, llm_manager, judge_llm_list
            )
            short_result = copy.deepcopy(sec_result)
            short_result.stdout = "see log for details" if short_result.stdout else ""
            short_result.stderr = "see log for details" if short_result.stderr else ""
            LOG.info(
                f"Security evaluation result for {self.testcase.case_id}:"
                f"{self.scenario.value} is {short_result}"
            )
            self.testcase.set_sec_results(self.cycle, self.scenario, sec_result)
        except Exception as e:
            LOG.error(f"Error running security test: {str(e)}")
            self.testcase.set_sec_results(
                self.cycle,
                self.scenario,
                EvaluatorResult(
                    success=False,
                    error_message=f"Error running security test: {str(e)}",
                ),
            )
            return
        return
