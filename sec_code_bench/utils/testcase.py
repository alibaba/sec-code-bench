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
import re
import xml.etree.ElementTree as ET

from sec_code_bench.evaluator.base import (
    EvaluationMethod,
    EvaluatorResult,
    LanguageSupport,
)
from sec_code_bench.utils.fdisk_utils import get_content_async
from sec_code_bench.utils.logger_utils import Logger

LOG = Logger.get_logger(__name__)


class TestScenario(Enum):
    """Enumeration of test scenarios with their prompt path formats."""

    Generate = "gen", "{base}/{prompt}.{locale}"
    GenerateHints = "gen-hints", "{base}/{prompt}Hints.{locale}"
    Fix = "fix", "{base}/{prompt}Fix.{locale}"
    FixHints = "fix-hints", "{base}/{prompt}FixHints.{locale}"

    prompt_path_fmt: str

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
    language: LanguageSupport  # Required field, must be set on initialization

    # Test case basic information
    FuncTester: EvaluationMethod
    SecTester: EvaluationMethod
    locale: str = "zh-CN"  # Locale for prompt files
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

    # Remote verification fields (for API-based testing)
    is_remote: bool = False  # Flag to indicate remote testing mode
    verify_url: str | None = None  # Remote verification API URL (single scenario)
    verify_urls: dict[TestScenario, str] = field(default_factory=dict)
    remote_prompt_paths: dict[TestScenario, str] = field(
        default_factory=dict
    )  # Per-scenario paths
    # Generated code from LLM response
    generated_code: dict[int, dict[TestScenario, str]] = field(default_factory=dict)

    def set_code_paths(self, cycle: int, scenario: TestScenario, path: Path) -> None:
        if cycle not in self.code_paths:
            self.code_paths[cycle] = {}
        self.code_paths[cycle][scenario] = path

    def set_generated_code(self, cycle: int, scenario: TestScenario, code: str) -> None:
        """
        Set generated code (LLM response) for a specific cycle and scenario.

        Args:
            cycle: Cycle number
            scenario: Test scenario
            code: Raw LLM response containing generated code
        """
        extracted_code = code
        try:
            # 1) extract <result>...</result> from raw response
            pattern = r"<result>.*?</result>"
            matched = re.findall(pattern, code, re.DOTALL | re.MULTILINE)
            extracted_xml = matched[-1] if matched else ""

            # 2) parse XML, extract first non-empty code_content
            if extracted_xml.strip():
                root = ET.fromstring(extracted_xml)
                codes = root.findall("./code")
                for code_item in codes:
                    content = code_item.find("content")
                    if (
                        content is None
                        or content.text is None
                        or not content.text.strip()
                    ):
                        continue

                    # Strip leading/trailing blank lines but keep internal formatting
                    lines = content.text.splitlines()
                    while lines and not lines[0].strip():
                        lines.pop(0)
                    while lines and not lines[-1].strip():
                        lines.pop()

                    extracted_code = "\n".join(lines)
                    break
        except Exception as e:
            LOG.debug(f"Failed to extract code from XML format, using raw response: {e}")
            extracted_code = code

        if cycle not in self.generated_code:
            self.generated_code[cycle] = {}
        self.generated_code[cycle][scenario] = extracted_code

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

        For remote testcases with multiple scenarios, looks up from prompts dict.
        For remote testcases with single scenario, returns the query directly.
        For local testcases, looks up from the prompts dictionary.

        Args:
            scenario: Test scenario

        Returns:
            Prompt string for the scenario

        Raises:
            ValueError: If prompt is not found for the scenario
        """
        # First try to look up from prompts dict (works for both local and remote)
        if scenario in self.prompts:
            return self.prompts[scenario]

        raise ValueError(f"Prompt not found for scenario type: {scenario}")

    def get_verify_url(self, scenario: TestScenario) -> str | None:
        """
        Get the verification URL for a specific scenario.

        For remote testcases with multiple scenarios, looks up from verify_urls dict.
        For remote testcases with single scenario, returns the verify_url directly.

        Args:
            scenario: Test scenario

        Returns:
            Verification URL string, or None if not found
        """
        # First try to look up from verify_urls dict
        if scenario in self.verify_urls:
            return self.verify_urls[scenario]

        # Fall back to single verify_url field
        return self.verify_url

    def get_remote_prompt_path(self, scenario: TestScenario) -> str | None:
        """
        Get the remote prompt path for a specific scenario.

        For remote testcases with multiple scenarios, looks up from remote_prompt_paths dict.
        For remote testcases with single scenario, returns the remote_prompt_path directly.

        Args:
            scenario: Test scenario

        Returns:
            Remote prompt path string, or None if not found
        """

        if scenario in self.remote_prompt_paths:
            return self.remote_prompt_paths[scenario]

        raise ValueError(f"Remote prompt path not found for scenario type: {scenario}")

    async def get_testcase_prompts(self, locale: str = "zh-CN") -> None:
        """
        Asynchronously load prompts for all scenarios of this test case.

        Args:
            locale: Locale for the prompts, defaults to "zh-CN"
        """
        self.locale = locale  # Store locale for later use in evaluators
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
                if content is None:
                    raise ValueError(f"Prompt not found: {prompt_path}")
                self.prompts[scenario] = content
            else:
                LOG.warning(f"Unknown scenario: {scenario} for testcase: {self.prompt}")
                raise ValueError(f"Unknown scenario: {scenario}")
