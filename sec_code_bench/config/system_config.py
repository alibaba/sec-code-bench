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

from pathlib import Path
from typing import Any

import yaml


class SystemConfig:
    """System configuration manager for SecCodeBench.

    This class loads and provides access to system-level configurations
    that control the behavior of the framework.
    """

    _instance: SystemConfig | None = None
    _config_data: dict[str, Any] = {}
    _config_loaded: bool = False

    def __new__(cls) -> SystemConfig:
        """Singleton pattern to ensure only one instance exists."""
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    @classmethod
    def load(cls, config_path: str | Path | None = None) -> SystemConfig:
        """Load system configuration from YAML file.

        Args:
            config_path: Path to the system configuration file.
                        If None, defaults to 'system_config.yaml' in project root.

        Returns:
            SystemConfig instance.

        Raises:
            FileNotFoundError: If the configuration file doesn't exist.
            yaml.YAMLError: If the configuration file is invalid.
        """
        instance = cls()

        # Only load once
        if cls._config_loaded:
            return instance

        if config_path is None:
            # Default to project root / system_config.yaml
            project_root = Path(__file__).parent.parent.parent
            config_path = project_root / "system_config.yaml"
        else:
            config_path = Path(config_path)

        if not config_path.exists():
            raise FileNotFoundError(
                f"System configuration file not found: {config_path}"
            )

        with open(config_path, "r", encoding="utf-8") as f:
            cls._config_data = yaml.safe_load(f) or {}

        cls._config_loaded = True
        return instance

    def get_languages_need_llm_judges(self) -> list[str]:
        """Get the list of languages that require LLM judges for evaluation.

        Returns:
            List of language identifiers (e.g., ['java']).
        """
        lang_reqs = self._config_data.get("language_requirements", {})
        return lang_reqs.get("languages_need_llm_judges", [])

    def requires_llm_judges(self, language: str) -> bool:
        """Check if a specific language requires LLM judges.

        Args:
            language: Language identifier (e.g., "java", "cpp").

        Returns:
            True if the language requires LLM judges, False otherwise.
        """
        languages = self.get_languages_need_llm_judges()
        return language.lower() in [lang.lower() for lang in languages]

    def get_category_weights(self) -> dict[str, float]:
        """Get category weights for score calculation.

        Returns:
            Dictionary of category weights (e.g., {"low": 1.0, "medium": 2.0, "high": 3.0}).
        """
        score_weights = self._config_data.get("score_weights", {})
        return score_weights.get("category_weights", {"low": 1.0, "medium": 2.0, "high": 3.0})

    def get_scenario_weights(self) -> dict[str, float]:
        """Get scenario weights for score calculation.

        Returns:
            Dictionary of scenario weights (e.g., {"gen": 1.0, "gen-hints": 1.0, "fix": 1.0, "fix-hints": 1.0}).
        """
        score_weights = self._config_data.get("score_weights", {})
        return score_weights.get("scenario_weights", {"gen": 1.0, "gen-hints": 1.0, "fix": 1.0, "fix-hints": 1.0})

    def get(self, key: str, default: Any = None) -> Any:
        """Get a configuration value by key.

        Args:
            key: Configuration key (supports dot notation, e.g., "language_requirements.languages_need_llm_judges").
            default: Default value if key doesn't exist.

        Returns:
            Configuration value or default.
        """
        keys = key.split(".")
        value = self._config_data

        for k in keys:
            if isinstance(value, dict):
                value = value.get(k)
                if value is None:
                    return default
            else:
                return default

        return value
