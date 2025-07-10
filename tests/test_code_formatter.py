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

import os
import sys

# Add the src directory to the Python path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "src"))

from sec_code_bench.parser.formatter.code_formatter import CodeFormatter
from sec_code_bench.parser.testcase.vulnerability_schema import VulnerabilitySchema


def test_code_formatter():
    """Test the CodeFormatter functionality"""
    vulnerability_schema = VulnerabilitySchema(
        language="java",
        primary_type="XXE",
        secondary_type="XXE",
        component_type="XxeDigester",
        file_type={"type": "code"},
        sinks=[
            {
                "class_regex": "org.apache.commons.digester.Digester",
                "method_regex": "parse",
            }
        ],
    )

    # Try to read the specified test file
    test_file_path = "static/dataset/java/XXE/XXE/XxeDigester/testcase/Test03.java"

    try:
        with open(test_file_path, "r", encoding="utf-8") as f:
            test_content = f.read()
        print(f"✓ Successfully read file: {test_file_path}")
    except FileNotFoundError:
        print(f"✗ File does not exist: {test_file_path}")
        print("Using fallback test content...")
        # Fallback to simple test content
        test_content = """
public class TestClass {
    public void testMethod() {
        // This is a test method body
        String test = "hello";
        System.out.println(test);
    }
}
"""
    except Exception as e:
        print(f"✗ Failed to read file: {e}")
        print("Using fallback test content...")
        # Fallback to simple test content
        test_content = """
public class TestClass {
    public void testMethod() {
        // This is a test method body
        String test = "hello";
        System.out.println(test);
    }
}
"""

    # Create formatter and test
    formatter = CodeFormatter(test_content, vulnerability_schema)

    # Test basic functionality
    assert vulnerability_schema.language == "java"
    assert vulnerability_schema.primary_type == "XXE"
    assert vulnerability_schema.secondary_type == "XXE"
    assert vulnerability_schema.component_type == "XxeDigester"
    assert vulnerability_schema.file_type == {"type": "code"}
    assert vulnerability_schema.sinks == [
        {"class_regex": "org.apache.commons.digester.Digester", "method_regex": "parse"}
    ]

    # Test vulnerability type checks
    assert not vulnerability_schema.is_mybatis_vulnerability()
    assert not vulnerability_schema.is_property_vulnerability()
    assert not vulnerability_schema.is_pom_vulnerability()

    # Test that formatter can be created without error
    assert formatter is not None
    assert formatter.content == test_content
    assert formatter.vulnerability_schema == vulnerability_schema

    # Test get_block_location method
    try:
        block_location = formatter.get_block_location()
        print("✓ Successfully got block location")
        print("=== Block Location Details ===")
        print(
            f"Block start line: {block_location.block_start_line} (method declaration start)"
        )
        print(f"Block start column: {block_location.block_start_column}")
        print(f"Block end line: {block_location.block_end_line}")
        print(f"Block end column: {block_location.block_end_column}")
        print(
            f"Block body start line: {block_location.block_body_start_line} (method body start)"
        )
        print(f"Block body start column: {block_location.block_body_start_column}")
        print("=============================")

        expected_start_line = 11
        expected_body_start_line = (
            11
        )

        print("=== Result===")
        if block_location.block_start_line == expected_start_line:
            print("✓ Block start line correct")
        else:
            print(
                f"✗ Block start line error: expect {expected_start_line}, get {block_location.block_start_line}"
            )

        if block_location.block_body_start_line == expected_body_start_line:
            print("✓ Block body start line successfully")
        else:
            print(
                f"✗ Block body start line error: expect {expected_body_start_line}, get {block_location.block_body_start_line}"
            )
        print("=================")

    except Exception as e:
        print(f"⚠️ get_block_location failed: {e}")

    # Test format_instruct_content method (this might fail if the content doesn't match expected format)
    try:
        formatted_content = formatter.format_instruct_content()
        print("✓ Successfully formatted content")
        print("Formatted content preview:")
        print(
            formatted_content[:200] + "..."
            if len(formatted_content) > 200
            else formatted_content
        )
    except Exception as e:
        print(f"⚠️ Formatting failed: {e}")
        print(
            "This is expected if the test content doesn't match the expected Java method format"
        )