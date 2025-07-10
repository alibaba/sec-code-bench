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

from sec_code_bench.checker.java.java_parser import JavaMethodCallFinder, MethodCallVisitor
from sec_code_bench.checker.java.model.features import MethodInvokeFeature
from sec_code_bench.parser.testcase.vulnerability_schema import VulnerabilitySchema


class TestJavaMethodCallFinderWithMethodInvokeFeature:
    """Test JavaMethodCallFinder with MethodInvokeFeature"""

    def test_java_method_call_finder_initialization_with_features(self):
        """Test JavaMethodCallFinder initialization with MethodInvokeFeature list"""
        # Create MethodInvokeFeature objects
        features = [
            MethodInvokeFeature("org.jdom2.input.SAXBuilder", "build"),
            MethodInvokeFeature("org.xml.sax.SAXParser", "parse"),
        ]

        # Initialize with features
        finder = JavaMethodCallFinder(target_features=features)

        assert finder.target_features == features
        assert len(finder.target_features) == 2
        assert (
            finder.target_features[0].get_class_regex() == "org.jdom2.input.SAXBuilder"
        )
        assert finder.target_features[0].get_method_regex() == "build"

    def test_java_method_call_finder_initialization_without_features(self):
        """Test JavaMethodCallFinder initialization without features"""
        finder = JavaMethodCallFinder()

        assert finder.target_features == []
        assert len(finder.target_features) == 0

    def test_set_target_features(self):
        """Test setting target features after initialization"""
        finder = JavaMethodCallFinder()

        # Create features
        features = [
            MethodInvokeFeature("org.jdom2.input.SAXBuilder", "build"),
            MethodInvokeFeature("org.xml.sax.SAXParser", "parse"),
        ]

        # Set features
        finder.set_target_features(features)

        assert finder.target_features == features
        assert len(finder.target_features) == 2

    def test_method_call_visitor_initialization_with_features(self):
        """Test MethodCallVisitor initialization with MethodInvokeFeature list"""
        # Create MethodInvokeFeature objects
        features = [
            MethodInvokeFeature("org.jdom2.input.SAXBuilder", "build"),
            MethodInvokeFeature("org.xml.sax.SAXParser", "parse"),
        ]

        # Initialize with features
        visitor = MethodCallVisitor(target_features=features)

        assert visitor.target_features == features
        assert len(visitor.target_features) == 2

    def test_method_call_visitor_initialization_without_features(self):
        """Test MethodCallVisitor initialization without features"""
        visitor = MethodCallVisitor()

        assert visitor.target_features == []
        assert len(visitor.target_features) == 0

    def test_set_target_features_on_visitor(self):
        """Test setting target features on visitor after initialization"""
        visitor = MethodCallVisitor()

        # Create features
        features = [
            MethodInvokeFeature("org.jdom2.input.SAXBuilder", "build"),
            MethodInvokeFeature("org.xml.sax.SAXParser", "parse"),
        ]

        # Set features
        visitor.set_target_features(features)

        assert visitor.target_features == features
        assert len(visitor.target_features) == 2

    def test_find_sinks_with_vulnerability_schema(self):
        """Test find_sinks method with VulnerabilitySchema containing sinks"""
        # Create VulnerabilitySchema with sinks
        vulnerability_schema = VulnerabilitySchema(
            language="java",
            primary_type="XXE",
            secondary_type="XXE",
            component_type="XxeSaxBuilder",
            file_type={"type": "code"},
            sinks=[
                {"class_regex": "org.jdom2.input.SAXBuilder", "method_regex": "build"},
                {"class_regex": "org.xml.sax.SAXParser", "method_regex": "parse"},
            ],
        )

        # Create finder
        finder = JavaMethodCallFinder()

        # Test with simple Java code that should not match
        java_code = """
        public class Test {
            public void test() {
                String str = "test";
                System.out.println(str);
            }
        }
        """

        # Should return 0 since no matching method calls
        result = finder.find_sinks(java_code, vulnerability_schema)
        assert result == 0

    def test_find_sinks_with_empty_sinks(self):
        """Test find_sinks method with VulnerabilitySchema containing empty sinks"""
        # Create VulnerabilitySchema with empty sinks
        vulnerability_schema = VulnerabilitySchema(
            language="java",
            primary_type="XXE",
            secondary_type="XXE",
            component_type="XxeSaxBuilder",
            file_type={"type": "code"},
            sinks=[],
        )

        # Create finder
        finder = JavaMethodCallFinder()

        # Test with Java code
        java_code = """
        public class Test {
            public void test() {
                String str = "test";
                System.out.println(str);
            }
        }
        """

        # Should return 0 since no sinks to match
        result = finder.find_sinks(java_code, vulnerability_schema)
        assert result == 0

    def test_find_sinks_with_none_sinks(self):
        """Test find_sinks method with VulnerabilitySchema containing None sinks"""
        # Create VulnerabilitySchema with None sinks
        vulnerability_schema = VulnerabilitySchema(
            language="java",
            primary_type="XXE",
            secondary_type="XXE",
            component_type="XxeSaxBuilder",
            file_type={"type": "code"},
            sinks=None,
        )

        # Create finder
        finder = JavaMethodCallFinder()

        # Test with Java code
        java_code = """
        public class Test {
            public void test() {
                String str = "test";
                System.out.println(str);
            }
        }
        """

        # Should return 0 since no sinks to match
        result = finder.find_sinks(java_code, vulnerability_schema)
        assert result == 0

    def test_find_sinks_with_matching_method_calls(self):
        """Test find_sinks method with Java code containing matching method calls"""
        # Create VulnerabilitySchema with sinks
        vulnerability_schema = VulnerabilitySchema(
            language="java",
            primary_type="XXE",
            secondary_type="XXE",
            component_type="XxeSaxBuilder",
            file_type={"type": "code"},
            sinks=[
                {"class_regex": "java.lang.String", "method_regex": "substring"},
                {"class_regex": "java.lang.String", "method_regex": "toLowerCase"},
            ],
        )

        # Create finder
        finder = JavaMethodCallFinder()

        # Test with Java code containing matching method calls
        java_code = """
        public class TestClass {
            public void testMethod() {
                // This should match: String.substring()
                String str = "test string";
                String sub = str.substring(0, 4);
                
                // This should match: String.toLowerCase()
                String lower = str.toLowerCase();
                
                // This should NOT match (different method name)
                String upper = str.toUpperCase();
                
                // This should NOT match (different class)
                System.out.println("test");
            }
        }
        """

        # Should return 2 since there are 2 matching method calls
        result = finder.find_sinks(java_code, vulnerability_schema)
        assert result == 2

    def test_find_sinks_with_partial_matches(self):
        """Test find_sinks method with partial matches (class matches but method doesn't)"""
        # Create VulnerabilitySchema with sinks
        vulnerability_schema = VulnerabilitySchema(
            language="java",
            primary_type="XXE",
            secondary_type="XXE",
            component_type="XxeSaxBuilder",
            file_type={"type": "code"},
            sinks=[
                {"class_regex": "java.lang.String", "method_regex": "substring"},
                {"class_regex": "java.lang.String", "method_regex": "toLowerCase"},
            ],
        )

        # Create finder
        finder = JavaMethodCallFinder()

        # Test with Java code containing partial matches
        java_code = """
        public class TestClass {
            public void testMethod() {
                // This should match: String.substring()
                String str = "test string";
                String sub = str.substring(0, 4);
                
                // This should match: String.toLowerCase()
                String lower = str.toLowerCase();
                
                // This should NOT match: String but different method
                String upper = str.toUpperCase();
                
                // This should NOT match: String but different method
                int length = str.length();
            }
        }
        """

        # Should return 2 since there are 2 matching method calls
        result = finder.find_sinks(java_code, vulnerability_schema)
        assert result == 2


if __name__ == "__main__":
    pytest.main([__file__])
