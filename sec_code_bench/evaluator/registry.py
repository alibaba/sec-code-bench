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


from sec_code_bench.evaluator.base import LanguageSupport
from sec_code_bench.evaluator.language_handler.base import (
    LanguageHandlerBase,
)
from sec_code_bench.evaluator.language_handler.java import JavaHandler


class HandlerFactory:
    """Factory class for getting processor instances based on programming language."""

    handlers: dict[str, type[LanguageHandlerBase]] = {
        "java": JavaHandler,
        # "python": PythonHandler,
        # "cpp": CppHandler,
        # etc.
    }

    @classmethod
    def get_handler(cls, language: str | LanguageSupport) -> LanguageHandlerBase:
        """Get the corresponding processor instance
        for the specified programming language.

        Args:
            language: Programming language identifier,
                can be a string or LanguageSupport enum value

        Returns:
            Processor instance for the corresponding language

        Raises:
            KeyError: When the specified language is not supported
        """
        if isinstance(language, LanguageSupport):  # Enum support
            language = language.value
        return cls.handlers[language.lower()]()
