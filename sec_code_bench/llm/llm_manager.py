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

from sec_code_bench.llm.llm_base import LLMBase, LLMConfig
from sec_code_bench.utils.logger_utils import Logger
from sec_code_bench.utils.rate_limiter import RateLimiter

LOG = Logger.get_logger(__name__)


class LLMManager:
    """Manage the lifecycle of LLM model instances."""

    def __init__(self) -> None:
        """Initialize the LLM manager."""
        self._models: dict[str, LLMBase] = {}
        self._registry: dict[str, type[LLMBase]] = {}

    def register_model_type(self, name: str, model_class: type[LLMBase]) -> None:
        """Register model type with the factory.

        Args:
            name: Name of the model type
            model_class: Model class to register

        Raises:
            TypeError: If model_class is not a subclass of LLMBase
        """
        if not issubclass(model_class, LLMBase):
            raise TypeError("Model type must inherit from LLMBase")
        self._registry[name] = model_class

    def create_instance(
        self,
        name: str,
        config: LLMConfig,
        rate_limit: RateLimiter | None = None,
        **kwargs,
    ) -> LLMBase:
        """Create and register a model instance.

        Args:
            name: Instance name
            config: Model configuration
            rate_limit: Rate limiter instance, if None a default
                rate limiter will be created
            **kwargs: Additional arguments for instance creation
        """
        if name in self._models:
            raise ValueError(f"Instance '{name}' already exists")

        model_class = self._registry.get(config.model)
        if not model_class:
            raise KeyError(f"Unregistered model type: {config.model}")

        # Default rate limiter: maximum 60 requests per minute
        if rate_limit is None:
            rate_limit = RateLimiter(max_cnts=60, window_seconds=60, burst_size=1)

        # Create instance with config and rate limiter
        instance = model_class(config, rate_limit, **kwargs)
        self._models[name] = instance
        return instance

    def get_instance(self, name: str) -> LLMBase | None:
        """Get a model instance.

        Args:
            name: Name of the instance to retrieve

        Returns:
            Model instance or None if not found
        """
        instance = self._models.get(name)
        if not instance:
            return None
        return instance

    def shutdown_all(self) -> None:
        for name, instance in list(self._models.items()):
            self._safe_close_instance(name, instance)
        self._models.clear()

    def _safe_close_instance(self, name: str, instance: LLMBase) -> None:
        """Safely close an instance with error handling.

        Args:
            name: Name of the instance
            instance: Instance to close
        """
        return instance.sync_close()
