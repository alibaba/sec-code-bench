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

from typing import Any, Dict, List


class SupplyChainFeature:
    """Supply chain feature class for describing dependency features"""

    def __init__(self, feature_name, group_id, artifact_id):
        self.feature_name = feature_name
        self.group_id = group_id
        self.artifact_id = artifact_id

    def get_feature_name(self):
        return self.feature_name

    def get_group_id(self):
        return self.group_id

    def get_artifact_id(self):
        return self.artifact_id


class MethodInvokeFeature:
    """Method invocation feature class for matching method calls"""

    def __init__(self, class_regex: str, method_regex: str):
        self.class_regex = class_regex
        self.method_regex = method_regex

    def get_class_regex(self) -> str:
        return self.class_regex

    def get_method_regex(self) -> str:
        return self.method_regex

    @staticmethod
    def from_sink(sink: Dict[str, Any]) -> "MethodInvokeFeature":
        """
        Create MethodInvokeFeature object from sink dictionary

        Args:
            sink: Dictionary containing class_regex and method_regex

        Returns:
            MethodInvokeFeature: Created method invocation feature object

        Raises:
            ValueError: When sink dictionary is missing required fields
        """
        if not isinstance(sink, dict):
            raise ValueError("sink must be a dictionary type")

        class_regex = sink.get("class_regex")
        method_regex = sink.get("method_regex")

        if class_regex is None or method_regex is None:
            raise ValueError(
                "sink dictionary must contain class_regex and method_regex fields"
            )

        return MethodInvokeFeature(class_regex=class_regex, method_regex=method_regex)

    @staticmethod
    def from_sinks(sinks: List[Dict[str, Any]]) -> List["MethodInvokeFeature"]:
        """
        Create MethodInvokeFeature object list from sink dictionary list

        Args:
            sinks: List of dictionaries containing class_regex and method_regex

        Returns:
            List[MethodInvokeFeature]: List of created method invocation feature objects
        """
        if not isinstance(sinks, list):
            raise ValueError("sinks must be a list type")

        features = []
        for sink in sinks:
            try:
                feature = MethodInvokeFeature.from_sink(sink)
                features.append(feature)
            except ValueError as e:
                # Log error but continue processing other sinks
                print(f"Warning: Skipping invalid sink: {e}")
                continue

        return features
