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

from collections.abc import Callable
from typing import Any, Protocol


class StatisticPlugin(Protocol):
    """
    Protocol for statistic plugin classes.

    Plugin classes should implement this protocol to
    provide statistic calculation functionality.
    """

    def calculate(
        self, model: str, testcases: list[Any], **kwargs: Any
    ) -> dict[str, Any]:
        """
        Calculate statistics for the given model and testcases.

        Args:
            model: Model name
            testcases: List of test cases
            **kwargs: Additional keyword arguments

        Returns:
            Dictionary containing calculated statistics
        """
        ...


# Function type for statistic functions
StatFunc = Callable[[str, list[Any]], dict[str, Any]]

# Union type for plugin types
PluginType = StatisticPlugin | StatFunc


def do_statistic(
    plugin: PluginType, model: str, testcases: list[Any], **kwargs: Any
) -> dict[str, Any]:
    """
    Execute statistic calculation using the provided plugin.

    Args:
        plugin: Statistic plugin (either class instance or function)
        model: Model name
        testcases: List of test cases
        **kwargs: Additional keyword arguments

    Returns:
        Dictionary containing calculated statistics
    """
    if hasattr(plugin, "calculate"):
        return plugin.calculate(model, testcases, **kwargs)
    return plugin(model, testcases, **kwargs)
