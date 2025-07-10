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

from abc import ABC, abstractmethod
from dataclasses import dataclass


@dataclass
class TestResult:
    tests: int
    failures: int
    errors: int
    skipped: int
    stdout: str
    stderr: str


class BaseTester(ABC):
    def __init__(self, code_path):
        self.code_path = code_path

    @abstractmethod
    def test(self) -> TestResult:
        pass
