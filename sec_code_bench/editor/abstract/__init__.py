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
from typing import Any


class Editor(ABC):
    """
    Abstract base class for code editors.

    This class defines the interface for different types of code editors
    that can be used to generate code based on prompts.
    """

    def __init__(self, timeout: int = 300) -> None:
        """
        Initialize the Editor instance.

        Args:
            timeout: Maximum time allowed for code generation in seconds (default: 300)
        """
        self.timeout = timeout

    @abstractmethod
    def coding(
        self, code_dir: str, prompt: str, need_prepare: bool = False, debug: bool = False
    ) -> None:
        """
        Generate code based on the given prompt.

        Args:
            code_dir: Running code directory
            prompt: The prompt to guide code generation
            need_prepare: Whether preparation steps are needed (default: False)
            debug: enable debug mode for application type editors (default: False)
        """
        pass

    @abstractmethod
    def _get_binary_name(self) -> str:
        """
        Get the name of the binary/executable.

        Returns:
            Name of the binary/executable
        """
        pass

    @abstractmethod
    def get_editor(self) -> str:
        """
        Get the editor name.

        Returns:
            Name of the editor
        """
        pass

    @abstractmethod
    def get_type(self) -> str:
        """
        Get the editor type.

        Returns:
            Type of the editor
        """
        pass

    @abstractmethod
    def __enter__(self) -> "Editor":
        """
        Enter the runtime context.

        Returns:
            Editor instance
        """
        pass

    @abstractmethod
    def __exit__(
        self,
        exc_type: type | None,
        exc_val: Exception | None,
        exc_tb: Any | None,
    ) -> bool | None:
        """
        Exit the runtime context.

        Args:
            exc_type: Exception type
            exc_val: Exception value
            exc_tb: Exception traceback

        Returns:
            Whether the exception was handled
        """
        pass
