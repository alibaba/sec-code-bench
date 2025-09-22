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

import json
from dataclasses import asdict, dataclass
from enum import Enum

from sec_code_bench.utils.logger_utils import Logger

LOG = Logger.get_logger(__name__)


class EvaluatorBase:
    """Base class for all evaluators.

    Contains common logic, labels, type markers, and basic validation methods.
    """

    def __init__(self, eval_type: Enum, tester_type: Enum, language: str) -> None:
        """Initialize the evaluator base with type information.

        Args:
            eval_type: The type of evaluation to perform.
            tester_type: The type of tester to use.
            language: The programming language being evaluated.
        """
        self.eval_type = eval_type
        self.tester_type = tester_type
        self.language = language


@dataclass
class EvaluatorResult:
    """Data class representing the result of an evaluation."""

    tests: int = 0
    failures: int = 0
    errors: int = 0
    skipped: int = 0
    stdout: str = ""
    stderr: str = ""
    success: bool = True
    error_message: str = ""

    def if_pass(self) -> bool:
        """Check if the evaluation passed.

        Returns:
            bool: True if the evaluation passed, False otherwise.
        """
        if not self.success:
            return False
        return self.failures == 0 and self.errors == 0

    def to_json(self):
        return json.dumps(asdict(self), ensure_ascii=False, indent=2)


class SyntaxCheckError(Exception):
    """Exception raised for syntax checking errors."""

    def __init__(self, message: str) -> None:
        """Initialize with an error message.

        Args:
            message: The error message.
        """
        super().__init__(message)


class FunctionCheckError(Exception):
    """Exception raised for function checking errors."""

    def __init__(self, message: str) -> None:
        """Initialize with an error message.

        Args:
            message: The error message.
        """
        super().__init__(message)


class EvaluationType(Enum):
    """Evaluation type enumeration."""

    Security = "Security"
    Function = "Function"


class EvaluationMethod(Enum):
    """Evaluation method enumeration."""

    UnitTest = "UnitTester"
    LLMTest = "LLMTester"


class LanguageSupport(Enum):
    """Supported programming languages."""

    JAVA = "java"
    PYTHON = "python"
    # JAVASCRIPT = "javascript"
    # C = "c"
    # CPP = "cpp"
    # GO = "go"
