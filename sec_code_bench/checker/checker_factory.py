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

"""
Code syntax checker factory class
"""

from typing import Dict, Type

from checker.base_checker import BaseChecker
from checker.java.java_checker import JavaChecker


class CheckerFactory:
    """
    Syntax checker factory class

    Used to get singleton instances of syntax checkers for different programming languages
    """

    # Registered checker types
    _checkers: Dict[str, Type[BaseChecker]] = {
        "java": JavaChecker,
        "Java": JavaChecker,
        "JAVA": JavaChecker,
    }

    @classmethod
    def register_checker(cls, language: str, checker_class: Type[BaseChecker]):
        """
        Register new syntax checker

        Args:
            language: Programming language name
            checker_class: Checker class
        """
        cls._checkers[language] = checker_class

    @classmethod
    def get_checker(cls, language: str, **kwargs) -> BaseChecker:
        """
        Get syntax checker singleton instance

        Args:
            language: Programming language name
            **kwargs: Additional parameters passed to checker constructor (only used on first creation)

        Returns:
            BaseChecker: Syntax checker singleton instance

        Raises:
            ValueError: When unsupported language
        """
        if language not in cls._checkers:
            raise ValueError(f"Unsupported programming language: {language}")

        checker_class = cls._checkers[language]
        return checker_class(**kwargs)

    @classmethod
    def create_checker(cls, language: str, **kwargs) -> BaseChecker:
        """
        Create syntax checker instance (deprecated, use get_checker instead)

        Args:
            language: Programming language name
            **kwargs: Additional parameters passed to checker constructor

        Returns:
            BaseChecker: Syntax checker instance

        Raises:
            ValueError: When unsupported language
        """
        # For backward compatibility, call get_checker method
        return cls.get_checker(language, **kwargs)

    @classmethod
    def get_supported_languages(cls) -> list:
        """
        Get list of supported programming languages

        Returns:
            list: List of supported programming languages
        """
        return list(set(cls._checkers.keys()))

    @classmethod
    def is_language_supported(cls, language: str) -> bool:
        """
        Check if specified programming language is supported

        Args:
            language: Programming language name

        Returns:
            bool: True if supported, False if not supported
        """
        return language in cls._checkers
