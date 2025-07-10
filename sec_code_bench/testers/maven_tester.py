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

import os
import subprocess
import xml.etree.ElementTree as ET

from typing_extensions import override

from .base_tester import BaseTester, TestResult


class MavenTester(BaseTester):
    def __init__(self, code_path):
        super().__init__(code_path)

    def _parse_java_junit_report(self, code_dir: str, result) -> TestResult:
        report_dir = f"{code_dir}/target/surefire-reports"
        tests = 0
        failures = 0
        errors = 0
        skipped = 0

        if not os.path.isdir(report_dir):
            print(
                f"Error: Directory '{report_dir}' not found. Did you run the tests first?"
            )
            return TestResult(0, 0, 0, 0, "", "")

        for filename in os.listdir(report_dir):
            if filename.startswith("TEST-") and filename.endswith(".xml"):
                filepath = os.path.join(report_dir, filename)
                try:
                    tree = ET.parse(filepath)
                    root = tree.getroot()

                    tests += int(root.attrib.get("tests", 0))
                    failures += int(root.attrib.get("failures", 0))
                    errors += int(root.attrib.get("errors", 0))
                    skipped += int(root.attrib.get("skipped", 0))
                except ET.ParseError:
                    print(f"Warning: Could not parse {filename}")

        return TestResult(
            tests, failures, errors, skipped, result.stdout, result.stderr
        )

    @override
    def test(self) -> TestResult:
        subprocess.run(["mvn", "clean", "-B", "-q"], cwd=self.code_path)
        result = subprocess.run(
            ["mvn", "test", "-B", "-q"],
            cwd=self.code_path,
            capture_output=True,
            timeout=60 * 5,
            text=True,
        )
        return self._parse_java_junit_report(self.code_path, result)
