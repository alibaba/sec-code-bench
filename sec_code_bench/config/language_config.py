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

from dataclasses import dataclass
from pathlib import Path


@dataclass
class LanguageConfig:
    """Configuration for a specific language evaluation.

    Attributes:
        language: Language identifier (e.g., "cpp", "nodejs", "java").
        benchmark_path: Path to the benchmark JSON file.
    """

    language: str
    benchmark_path: Path

    @classmethod
    def from_string(cls, config_str: str) -> LanguageConfig:
        """Parse a language config string.

        Supported formats:
        - 'language:locale:benchmark_path' - Full format (for local mode)
        - 'language:benchmark_path' - Short format (locale optional, for remote mode)

        Args:
            config_str: Config string like 'cpp:en-US:./benchmark/cpp/cpp.json'
                       or 'python:./benchmark/python/python-remote.json'

        Returns:
            LanguageConfig instance.

        Raises:
            ValueError: If the format is invalid.
        """
        parts = config_str.split(":")
        if len(parts) == 3:
            # Full format: language:locale:benchmark_path
            language, _, benchmark_path = parts
        elif len(parts) == 2:
            # Short format: language:benchmark_path (locale defaults to empty)
            language, benchmark_path = parts
        else:
            raise ValueError(
                f"Invalid lang-config format: '{config_str}'. "
                "Expected: 'language:locale:benchmark_path' or 'language:benchmark_path'"
            )
        return cls(
            language=language.lower(),
            benchmark_path=Path(benchmark_path),
        )
