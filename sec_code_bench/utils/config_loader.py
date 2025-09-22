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

import configparser
import re
from collections.abc import Callable
from typing import Any


class ConfigValidationError(Exception):
    """Configuration validation exception, containing all error messages"""

    def __init__(self, errors: list[str]):
        """
        Initialize configuration validation exception

        Args:
            errors (list[str]): List of error messages
        """
        self.errors = errors
        super().__init__("\n".join(errors))


class ConfigLoader:
    def __init__(
        self,
        validation_rules: dict[str, dict[str, Any]] | None = None,
        auto_validate: bool = True,
    ):
        """
        INI configuration file loader

        Args:
            validation_rules (Optional[Dict[str, Dict[str, Any]]], optional):
                Validation rules dictionary. Defaults to None.
            auto_validate (bool, optional): Whether to automatically perform
                full validation after loading. Defaults to True.
        """
        self.validation_rules = validation_rules or {}
        self.auto_validate = auto_validate
        self.raw_config: dict[str, dict[str, str]] = {}
        self.typed_config: dict[str, dict[str, Any]] = {}
        self.errors: list[str] = []

    def load(self, file_path: str) -> None:
        """
        Load configuration and perform basic type conversion

        This method reads the specified INI configuration file, parses
        its content, and performs type conversion based on predefined
        validation rules. If auto_validate is set to True, it will
        also perform full configuration validation.

        Args:
            file_path (str): Configuration file path

        Raises:
            ConfigValidationError: Raised when configuration validation fails
            FileNotFoundError: Raised when configuration file does not exist
        """
        parser = configparser.ConfigParser()
        parser.read(file_path, encoding="utf-8")

        self.raw_config = {
            section: dict(parser.items(section)) for section in parser.sections()
        }

        # Always perform basic type conversion
        self._convert_types()

        # Perform full validation as needed
        if self.auto_validate:
            self.full_validate()

    def get(self, section: str, option: str, default: Any = None) -> Any:
        """
        Get type-converted configuration value

        Return the type-converted configuration value based on the
        specified section and option name. If no validation rules are set,
        return the original string value.

        Args:
            section (str): Configuration section name
            option (str): Configuration option name
            default (Any, optional): Default value to return when
                configuration option does not exist. Defaults to None.

        Returns:
            Any: Type-converted configuration value or default value
        """
        if not self.validation_rules:
            return self.raw_config.get(section, {}).get(option, default)
        return self.typed_config.get(section, {}).get(option, default)

    def _convert_types(self) -> None:
        """
        Core type conversion method

        Convert original string configuration values to target types
        based on predefined validation rules. If conversion fails and
        the configuration option has a default value, use the default value.
        """
        self.typed_config = {}
        self.errors = []

        for section, options_rules in self.validation_rules.items():
            if section not in self.raw_config:
                continue  # Section existence validation is left to full_validate

            self.typed_config[section] = {}
            section_data = self.raw_config[section]

            for option, rules in options_rules.items():
                value_str = section_data.get(option)
                target_type = rules.get("type", str)

                # Handle default values
                if value_str is None and "default" in rules:
                    default_val = rules["default"]
                    if not isinstance(default_val, target_type):
                        self.errors.append(
                            f"[{section}] {option} default value type mismatch, "
                            f"expected {target_type.__name__}, "
                            f"actual {type(default_val).__name__}"
                        )
                        continue
                    self.typed_config[section][option] = default_val
                    continue

                try:
                    converted = self._convert_type(value_str, target_type)
                    self.typed_config[section][option] = converted
                except (ValueError, TypeError) as e:
                    self.errors.append(f"[{section}] {option} {str(e)}")
                    if "default" in rules:
                        self.typed_config[section][option] = rules["default"]

    def full_validate(self) -> None:
        """
        Full configuration validation (optional execution)

        Perform complete configuration validation, including:
        1. Check if required configuration sections exist
        2. Check if required configuration options exist
        3. Check numeric ranges
        4. Check allowed values
        5. Perform regex validation
        6. Execute custom validation functions

        Raises:
            ConfigValidationError: Raised when configuration validation fails
        """
        new_errors = []

        # 1. Check required sections
        for section in self.validation_rules:
            if section not in self.raw_config:
                new_errors.append(f"Missing required configuration section: [{section}]")

        # 2. Check each configuration option rules
        for section, options_rules in self.validation_rules.items():
            if section not in self.raw_config:
                continue

            section_data = self.raw_config[section]
            typed_section = self.typed_config.get(section, {})

            for option, rules in options_rules.items():
                value = typed_section.get(option)
                value_str = section_data.get(option)

                # Required validation (check raw data first)
                if rules.get("required", False) and value_str is None:
                    new_errors.append(f"[{section}] {option} is required")

                # Skip subsequent validation if value is None and no default value
                if value is None and "default" not in rules:
                    continue

                # Numeric range check
                if isinstance(value, (int | float)):
                    if "min" in rules and value < rules["min"]:
                        new_errors.append(
                            f"[{section}] {option} value {value} "
                            f"is less than minimum {rules['min']}"
                        )
                    if "max" in rules and value > rules["max"]:
                        new_errors.append(
                            f"[{section}] {option} value {value} "
                            f"is greater than maximum {rules['max']}"
                        )

                # Allowed values check
                if "allowed" in rules and value not in rules["allowed"]:
                    allowed = ", ".join(map(str, rules["allowed"]))
                    new_errors.append(
                        f"[{section}] {option} value {value} "
                        f"is not in allowed range: {allowed}"
                    )

                # Regex check
                if (
                    "regex" in rules
                    and isinstance(value, str)
                    and not re.fullmatch(rules["regex"], value)
                ):
                    new_errors.append(
                        f"[{section}] {option} value '{value}' "
                        "does not match format requirements"
                    )

                # Custom validation
                if "custom" in rules:
                    try:
                        custom_validator: Callable[[Any], bool] = rules["custom"]
                        if not custom_validator(value):
                            new_errors.append(
                                f"[{section}] {option} {value} failed custom validation"
                            )
                    except Exception as e:
                        new_errors.append(
                            f"[{section}] {option} validation execution error: {str(e)}"
                        )

        # Merge errors and raise
        all_errors = self.errors + new_errors
        if all_errors:
            raise ConfigValidationError(all_errors)

    def _convert_type(self, value: str | None, target_type: type) -> Any:
        """
        Type conversion with error tolerance

        Convert string values to target types, supporting special format processing
        (such as thousand-separated numbers) and boolean conversion.

        Args:
            value (Optional[str]): String value to convert
            target_type (type): Target type (int, float, bool, str, etc.)

        Returns:
            Any: Converted value

        Raises:
            ValueError: Raised when conversion fails
            TypeError: Raised when type is not supported
        """
        if value is None:
            raise ValueError("Value cannot be empty")

        value = value.strip()

        if target_type is bool:
            return self._str_to_bool(value)

        try:
            # Handle thousand-separated numbers
            if target_type is int and "," in value:
                return int(value.replace(",", ""))
            if target_type is float and "," in value:
                return float(value.replace(",", ""))

            return target_type(value)
        except ValueError as err:
            raise ValueError(
                f"Cannot convert '{value}' to {target_type.__name__} type"
            ) from err

    def _str_to_bool(self, value: str) -> bool:
        """
        Enhanced boolean conversion

        Support conversion of multiple true/false value representations.

        Args:
            value (str): String value to convert

        Returns:
            bool: Converted boolean value

        Raises:
            ValueError: Raised when boolean value cannot be recognized
        """
        lower_val = value.lower().strip()
        true_values = ["true", "yes", "on", "1", "enable", "enabled"]
        false_values = ["false", "no", "off", "0", "disable", "disabled"]

        if lower_val in true_values:
            return True
        if lower_val in false_values:
            return False

        raise ValueError(f"Unrecognized boolean value: {value}")
