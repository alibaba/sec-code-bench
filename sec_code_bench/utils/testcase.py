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

from dataclasses import dataclass, field
from enum import Enum
from pathlib import Path

from sec_code_bench.evaluator.base import (
    EvaluationMethod,
    EvaluatorResult,
    LanguageSupport,
)
from sec_code_bench.utils.fdisk_utils import get_content, get_content_async
from sec_code_bench.utils.logger_utils import Logger

LOG = Logger.get_logger(__name__)


class TestScenario(Enum):
    """Enumeration of test scenarios with their prompt path formats."""

    Generate = "gen", "{base}/{prompt}.{locale}"
    GenerateHints = "gen-hints", "{base}/{prompt}Hints.{locale}"
    Fix = "fix", "{base}/{prompt}Fix.{locale}"
    FixHints = "fix-hints", "{base}/{prompt}FixHints.{locale}"

    # Add more scenarios as needed

    def __new__(cls, value: str, prompt_path_fmt: str):
        """
        Create a new TestScenario enum value.

        Args:
            value: The string value of the enum
            prompt_path_fmt: Format string for the prompt path
        """
        obj = object.__new__(cls)
        obj._value_ = value
        obj.prompt_path_fmt = prompt_path_fmt
        return obj

    def __str__(self) -> str:
        """Return the string representation of the enum value."""
        return self.value

    def __repr__(self) -> str:
        """Return the representation of the enum value."""
        return f"TestScenario.{self.name}"


@dataclass
class Testcase:
    """
    Test case class containing basic information,
    prompt templates, and evaluation methods.
    """

    case_id: str  # Unique identifier

    # Test case basic information
    FuncTester: EvaluationMethod | None = None
    SecTester: EvaluationMethod | None = None
    language: LanguageSupport | None = None
    template: str = ""
    severity: str = ""
    # If not in scenarios, prompt will be empty
    prompt: str = ""
    prompts: dict[TestScenario, str] = field(default_factory=dict)
    scenarios: list[TestScenario] = field(default_factory=list)
    params: dict[str, str] = field(default_factory=dict)
    # Evaluate results for each model-scenario combination
    # [cycle, scenario, EvaluatorResult]
    FunResults: dict[int, dict[TestScenario, EvaluatorResult]] = field(
        default_factory=dict
    )
    SecResults: dict[int, dict[TestScenario, EvaluatorResult]] = field(
        default_factory=dict
    )
    code_paths: dict[int, dict[TestScenario, Path]] = field(default_factory=dict)
    score: dict[TestScenario, float] = field(default_factory=dict)

    def set_code_paths(self, cycle: int, scenario: TestScenario, path: Path) -> None:
        if cycle not in self.code_paths:
            self.code_paths[cycle] = {}
        self.code_paths[cycle][scenario] = path

    def set_fun_results(
        self, cycle: int, scenario: TestScenario, result: EvaluatorResult
    ) -> None:
        """
        Set functional test results for a specific cycle and scenario.

        Args:
            cycle: Cycle number
            scenario: Test scenario
            result: Evaluation result
        """
        if cycle not in self.FunResults:
            self.FunResults[cycle] = {}
        self.FunResults[cycle][scenario] = result

    def set_sec_results(
        self, cycle: int, scenario: TestScenario, result: EvaluatorResult
    ) -> None:
        """
        Set security test results for a specific cycle and scenario.

        Args:
            cycle: Cycle number
            scenario: Test scenario
            result: Evaluation result
        """
        if cycle not in self.SecResults:
            self.SecResults[cycle] = {}
        self.SecResults[cycle][scenario] = result

    def set_error_result(
        self, cycle: int, scenario: TestScenario, error_message: str
    ) -> None:
        """
        Set error result for both functional and security tests.

        Args:
            cycle: Cycle number
            scenario: Test scenario
            error_message: Error message to set
        """
        self.set_fun_results(
            cycle,
            scenario,
            EvaluatorResult(
                success=False,
                error_message=error_message,
            ),
        )
        self.set_sec_results(
            cycle,
            scenario,
            EvaluatorResult(
                success=False,
                error_message=error_message,
            ),
        )

    def get_scenario_prompt(self, scenario: TestScenario) -> str:
        """
        Get the prompt for a specific scenario.

        Args:
            scenario: Test scenario

        Returns:
            Prompt string for the scenario

        Raises:
            ValueError: If prompt is not found for the scenario
        """
        # Directly look up the relevant prompt from the dictionary
        try:
            return self.prompts[scenario]
        except KeyError as e:
            raise ValueError(f"Prompt not found for scenario type: {scenario}") from e

    def get_testcase_prompts_sync(self, locale: str = "zh-CN") -> None:
        """
        Synchronously load prompts for all scenarios of this test case.

        Args:
            locale: Locale for the prompts, defaults to "zh-CN"
        """
        current_dir = Path(__file__).parent.parent.parent
        prompt_base_dir = (
            f"{current_dir}/datasets/benchmark/{self.language.value}/prompts"
        )

        for scenario in self.scenarios:
            path_fmt = scenario.prompt_path_fmt
            if path_fmt:
                prompt_path = path_fmt.format(
                    base=prompt_base_dir, prompt=self.prompt, locale=locale
                )
                content = get_content(prompt_path)
                self.prompts[scenario] = content
            else:
                LOG.warning(f"Unknown scenario: {scenario} for testcase: {self.prompt}")
        # TODO: 空prompt报错！

    async def get_testcase_prompts(self, locale: str = "zh-CN") -> None:
        """
        Asynchronously load prompts for all scenarios of this test case.

        Args:
            locale: Locale for the prompts, defaults to "zh-CN"
        """
        current_dir = Path(__file__).parent.parent.parent
        prompt_base_dir = (
            f"{current_dir}/datasets/benchmark/{self.language.value}/prompts"
        )

        for scenario in self.scenarios:
            path_fmt = scenario.prompt_path_fmt
            if path_fmt:
                prompt_path = path_fmt.format(
                    base=prompt_base_dir, prompt=self.prompt, locale=locale
                )
                content = await get_content_async(prompt_path)
                self.prompts[scenario] = content
            else:
                LOG.warning(f"Unknown scenario: {scenario} for testcase: {self.prompt}")
        # TODO: 空prompt报错！
