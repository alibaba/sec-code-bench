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
Base class for code syntax checkers
"""

from abc import ABC, abstractmethod
from typing import TypeVar

T = TypeVar("T")


class SingletonMeta(type):
    """
    Singleton pattern metaclass
    """

    _instances = {}

    def __call__(cls, *args, **kwargs):
        if cls not in cls._instances:
            cls._instances[cls] = super().__call__(*args, **kwargs)
        return cls._instances[cls]


class ABCSingletonMeta(SingletonMeta, type(ABC)):
    """
    Metaclass combining ABC and singleton pattern
    """

    pass


class BaseChecker(ABC, metaclass=ABCSingletonMeta):
    """
    Base class for code syntax checkers

    Provides unified syntax checking interface for different programming languages
    Uses singleton pattern to ensure only one instance per language checker
    """

    def __init__(self, language: str):
        """
        Initialize base class

        Args:
            language: Programming language name
        """
        # Avoid duplicate initialization
        if hasattr(self, "_initialized"):
            return

        self.language = language
        self._initialized = True

    @abstractmethod
    def check_syntax_from_content(
        self, code_content: str, vulnerability_schema=None
    ) -> bool:
        """
        Check syntax correctness of code content

        Args:
            code_content: Code content string
            vulnerability_schema: Vulnerability schema object for context-specific checking

        Returns:
            bool: True if syntax is correct, False if syntax error

        Raises:
            Exception: Exceptions during checking process
        """
        pass

    @abstractmethod
    def check_sink_in(self, code_content: str, vulnerability_schema=None) -> bool:
        """
        Check if sink patterns exist in code content

        Args:
            code_content: Code content string to check
            vulnerability_schema: Vulnerability schema object containing sink patterns

        Returns:
            bool: True if sink patterns found, False otherwise
        """
        pass

    def __str__(self) -> str:
        return f"{self.__class__.__name__}({self.language})"

    def __repr__(self) -> str:
        return self.__str__()
