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

import pytest

from sec_code_bench.checker.checker_factory import CheckerFactory


class TestCheckerFactory:
    """Test CheckerFactory with singleton pattern"""

    def test_language_support_check(self):
        """Test checking if language is supported"""
        language = "java"

        # Check if language is supported first
        assert CheckerFactory.is_language_supported(language)
        print(f"Language '{language}' is supported")

    def test_get_checker_success(self):
        """Test getting checker for supported language"""
        language = "java"

        # Get checker if supported
        checker = CheckerFactory.get_checker(language)
        assert checker is not None
        print(f"Successfully got checker: {checker}")

    def test_singleton_pattern(self):
        """Test singleton pattern - get another instance"""
        language = "java"

        # Get first checker
        checker = CheckerFactory.get_checker(language)

        # Test singleton pattern - get another instance
        another_checker = CheckerFactory.get_checker(language)

        # They should be the same instance
        assert checker is another_checker
        print(f"Got another checker: {another_checker}")
        print(f"Are they the same instance? {checker is another_checker}")

    def test_different_case(self):
        """Test with different case"""
        # Get checker with lowercase
        checker = CheckerFactory.get_checker("java")

        # Test with different case
        java_upper = CheckerFactory.get_checker("Java")

        # They should be the same instance
        assert checker is java_upper
        print(f"Got Java checker: {java_upper}")
        print(f"Same as 'java' checker? {checker is java_upper}")

    def test_unsupported_language(self):
        """Test unsupported language"""
        unsupported_lang = "python"

        # Check if unsupported language is supported
        assert not CheckerFactory.is_language_supported(unsupported_lang)
        print(f"Language '{unsupported_lang}' is not supported")

    def test_unsupported_language_raises_error(self):
        """Test that getting unsupported language raises ValueError"""
        unsupported_lang = "python"

        with pytest.raises(
            ValueError, match=f"Unsupported programming language: {unsupported_lang}"
        ):
            CheckerFactory.get_checker(unsupported_lang)

    def test_supported_languages_list(self):
        """Test getting list of supported languages"""
        supported_languages = CheckerFactory.get_supported_languages()

        # Should contain java variations
        assert "java" in supported_languages
        assert "Java" in supported_languages
        assert "JAVA" in supported_languages

        print(f"Supported languages: {supported_languages}")

    def test_backward_compatibility(self):
        """Test backward compatibility with create_checker method"""
        # Test backward compatibility
        old_style_checker = CheckerFactory.create_checker("java")
        new_style_checker = CheckerFactory.get_checker("java")

        # They should be the same instance
        assert old_style_checker is new_style_checker
        print(f"create_checker result: {old_style_checker}")
        print(f"get_checker result: {new_style_checker}")
        print(f"Are they the same instance? {old_style_checker is new_style_checker}")

    def test_register_checker(self):
        """Test registering new checker"""

        # Create a mock checker class
        class MockChecker:
            def __init__(self, **kwargs):
                self.kwargs = kwargs

        # Register the mock checker
        CheckerFactory.register_checker("mock", MockChecker)

        # Check if it's now supported
        assert CheckerFactory.is_language_supported("mock")

        # Get the checker
        checker = CheckerFactory.get_checker("mock", test_param="test_value")
        assert isinstance(checker, MockChecker)
        assert checker.kwargs["test_param"] == "test_value"
