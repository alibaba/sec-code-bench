# Copyright (c) 2025 Alibaba Group and/or its affiliates.

#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at

#      http://www.apache.org/licenses/LICENSE-2.0

#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import configparser
from pathlib import Path
from typing import Any, Dict, Optional


class ConfigManager:
    """
    Configuration manager for reading and managing project settings from .ini file
    """

    def __init__(self, config_file: str = "config.ini"):
        """
        Initialize configuration manager

        Args:
            config_file: Path to the configuration file
        """
        self.config_file = Path(config_file)
        self.config = configparser.ConfigParser()
        self._load_config()

    def _load_config(self):
        """Load configuration from .ini file"""
        if not self.config_file.exists():
            raise FileNotFoundError(f"Configuration file not found: {self.config_file}")

        try:
            self.config.read(self.config_file, encoding="utf-8")
        except configparser.Error as e:
            raise ValueError(f"Failed to parse configuration file: {e}")

    def get(self, section: str, key: str, fallback: Optional[Any] = None) -> str:
        """
        Get configuration value

        Args:
            section: Configuration section name
            key: Configuration key name
            fallback: Fallback value if key doesn't exist

        Returns:
            Configuration value as string
        """
        return self.config.get(section, key, fallback=fallback)

    def getint(self, section: str, key: str, fallback: Optional[int] = None) -> int:
        """
        Get configuration value as integer

        Args:
            section: Configuration section name
            key: Configuration key name
            fallback: Fallback value if key doesn't exist

        Returns:
            Configuration value as integer
        """
        return self.config.getint(section, key, fallback=fallback)

    def getboolean(
        self, section: str, key: str, fallback: Optional[bool] = None
    ) -> bool:
        """
        Get configuration value as boolean

        Args:
            section: Configuration section name
            key: Configuration key name
            fallback: Fallback value if key doesn't exist

        Returns:
            Configuration value as boolean
        """
        return self.config.getboolean(section, key, fallback=fallback)

    def getfloat(
        self, section: str, key: str, fallback: Optional[float] = None
    ) -> float:
        """
        Get configuration value as float

        Args:
            section: Configuration section name
            key: Configuration key name
            fallback: Fallback value if key doesn't exist

        Returns:
            Configuration value as float
        """
        return self.config.getfloat(section, key, fallback=fallback)

    def get_section(self, section: str) -> Dict[str, str]:
        """
        Get all key-value pairs from a section

        Args:
            section: Configuration section name

        Returns:
            Dictionary of key-value pairs
        """
        if not self.config.has_section(section):
            return {}

        return dict(self.config.items(section))

    def has_section(self, section: str) -> bool:
        """
        Check if section exists

        Args:
            section: Configuration section name

        Returns:
            True if section exists, False otherwise
        """
        return self.config.has_section(section)

    def has_option(self, section: str, key: str) -> bool:
        """
        Check if option exists in section

        Args:
            section: Configuration section name
            key: Configuration key name

        Returns:
            True if option exists, False otherwise
        """
        return self.config.has_option(section, key)

    def reload(self):
        """Reload configuration from file"""
        self._load_config()

    def __repr__(self):
        return f"ConfigManager(config_file='{self.config_file}')"


# Global configuration manager instance
_config_manager: Optional[ConfigManager] = None


def get_config_manager(config_file: str = "config.ini") -> ConfigManager:
    """
    Get global configuration manager instance

    Args:
        config_file: Path to configuration file (only used on first call)

    Returns:
        Global ConfigManager instance
    """
    global _config_manager
    if _config_manager is None:
        _config_manager = ConfigManager(config_file)
    return _config_manager


def reload_config(config_file: str = "config.ini"):
    """
    Reload global configuration

    Args:
        config_file: Path to configuration file
    """
    global _config_manager
    _config_manager = ConfigManager(config_file)
