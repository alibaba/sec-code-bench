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

from sec_code_bench.checker.java.model.features import MethodInvokeFeature


class TestMethodInvokeFeature:
    """TestMethodInvokeFeature functionality test class"""

    def test_from_sink_valid(self):
        """Test creating MethodInvokeFeature from valid sink dictionary"""
        sink = {"class_regex": "org.jdom2.input.SAXBuilder", "method_regex": "build"}

        feature = MethodInvokeFeature.from_sink(sink)

        assert isinstance(feature, MethodInvokeFeature)
        assert feature.get_class_regex() == "org.jdom2.input.SAXBuilder"
        assert feature.get_method_regex() == "build"

    def test_from_sink_missing_class_regex(self):
        """Test exception when class_regex field is missing"""
        sink = {"method_regex": "build"}

        with pytest.raises(
            ValueError,
            match="sink dictionary must contain class_regex and method_regex fields",
        ):
            MethodInvokeFeature.from_sink(sink)

    def test_from_sink_missing_method_regex(self):
        """Test exception when method_regex field is missing"""
        sink = {"class_regex": "org.jdom2.input.SAXBuilder"}

        with pytest.raises(
            ValueError,
            match="sink dictionary must contain class_regex and method_regex fields",
        ):
            MethodInvokeFeature.from_sink(sink)

    def test_from_sink_invalid_type(self):
        """Test exception when non-dictionary type is passed"""
        sink = "invalid_sink"

        with pytest.raises(ValueError, match="sink must be a dictionary type"):
            MethodInvokeFeature.from_sink(sink)

    def test_from_sinks_valid(self):
        """Test creating MethodInvokeFeature list from valid sink list"""
        sinks = [
            {"class_regex": "org.jdom2.input.SAXBuilder", "method_regex": "build"},
            {"class_regex": "org.xml.sax.SAXParser", "method_regex": "parse"},
        ]

        features = MethodInvokeFeature.from_sinks(sinks)

        assert len(features) == 2
        assert all(isinstance(feature, MethodInvokeFeature) for feature in features)

        assert features[0].get_class_regex() == "org.jdom2.input.SAXBuilder"
        assert features[0].get_method_regex() == "build"
        assert features[1].get_class_regex() == "org.xml.sax.SAXParser"
        assert features[1].get_method_regex() == "parse"

    def test_from_sinks_with_invalid_sink(self):
        """Test handling list with invalid sink"""
        sinks = [
            {"class_regex": "org.jdom2.input.SAXBuilder", "method_regex": "build"},
            {
                "class_regex": "org.xml.sax.SAXParser"
                # Missing method_regex
            },
            {"class_regex": "org.dom4j.io.SAXReader", "method_regex": "read"},
        ]

        features = MethodInvokeFeature.from_sinks(sinks)

        # Should only return valid features
        assert len(features) == 2
        assert features[0].get_class_regex() == "org.jdom2.input.SAXBuilder"
        assert features[1].get_class_regex() == "org.dom4j.io.SAXReader"

    def test_from_sinks_invalid_type(self):
        """Test exception when non-list type is passed"""
        sinks = "invalid_sinks"

        with pytest.raises(ValueError, match="sinks must be a list type"):
            MethodInvokeFeature.from_sinks(sinks)

    def test_from_sinks_empty_list(self):
        """Test handling empty list"""
        sinks = []

        features = MethodInvokeFeature.from_sinks(sinks)

        assert len(features) == 0
        assert isinstance(features, list)


if __name__ == "__main__":
    pytest.main([__file__])
