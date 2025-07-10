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

import os

import pytest

from sec_code_bench.parser.testcase.testcase_manager import TestcaseManager
from sec_code_bench.parser.testcase.vulnerability_schema import VulnerabilitySchema
from sec_code_bench.utils.llm_utils import create_llm_client


class TestTestcaseManager:
    """TestcaseManager functionality test class"""

    @pytest.fixture
    def vulnerability_schema(self):
        """Create test VulnerabilitySchema instance"""
        return VulnerabilitySchema(
            language="java",
            primary_type="SecurityMisconfiguration",
            secondary_type="SecurityMisconfiguration",
            component_type="SpringBootActuator",
            file_type={"type": "properties"},
            sinks=[
                {
                    "class_regex": "management\\.security\\.enabled",
                    "method_regex": ".*",
                },
                {"class_regex": "endpoints\\.sensitive", "method_regex": ".*"},
                {"class_regex": "endpoints\\.enabled", "method_regex": ".*"},
                {"class_regex": "endpoints\\.info\\.enabled", "method_regex": ".*"},
                {"class_regex": "endpoints\\.health\\.enabled", "method_regex": ".*"},
                {
                    "class_regex": "endpoints\\.env\\.post\\.enabled",
                    "method_regex": ".*",
                },
                {"class_regex": "endpoints\\.env\\.enabled", "method_regex": ".*"},
                {"class_regex": "endpoints\\.heapdump\\.enabled", "method_regex": ".*"},
                {"class_regex": "management\\.port", "method_regex": ".*"},
                {"class_regex": "management\\.address", "method_regex": ".*"},
                {
                    "class_regex": "management\\.endpoints\\.web\\.exposure\\.include",
                    "method_regex": ".*",
                },
                {
                    "class_regex": "management\\.endpoints\\.web\\.exposure\\.exclude",
                    "method_regex": ".*",
                },
                {
                    "class_regex": "management\\.endpoint\\.env\\.post\\.enabled",
                    "method_regex": ".*",
                },
                {"class_regex": "management\\.server\\.port", "method_regex": ".*"},
                {"class_regex": "management\\.server\\.address", "method_regex": ".*"},
            ],
        )

    @pytest.fixture
    def testcase_manager(self, vulnerability_schema):
        """Create TestcaseManager instance"""
        return TestcaseManager(vulnerability_schema, "datasets/static/dataset/java")

    @pytest.fixture
    def llm_client(self):
        """Create LLM client"""
        return create_llm_client(
            api_key=os.getenv("DASHSCOPE_API_KEY"),
            base_url=os.getenv("DASHSCOPE_ENDPOINT"),
            is_async_client=False,
        )

    def test_initialization(self, testcase_manager, vulnerability_schema):
        """Test TestcaseManager initialization"""
        print("\n【Step 1】Initialize TestcaseManager")

        assert testcase_manager.vulnerability_schema == vulnerability_schema
        assert testcase_manager.dataset_dir.exists()
        print(
            f"✓ Initialization complete, dataset directory: {testcase_manager.dataset_dir}"
        )
        print(f"✓ Set vulnerability type: {testcase_manager}")
        print(f"✓ Target path: {testcase_manager.get_target_path()}")
        print(f"✓ File type: {vulnerability_schema.file_type}")
        print(f"✓ Dangerous functions: {len(vulnerability_schema.sinks)} items")

    def test_get_target_path(self, testcase_manager):
        """Test getting target path"""
        target_path = testcase_manager.get_target_path()
        assert target_path.exists(), f"Target path does not exist: {target_path}"
        print(f"✓ Target path exists: {target_path}")

    def test_get_file_paths(self, testcase_manager):
        """Test getting file paths"""
        file_paths = testcase_manager._get_file_paths()
        assert isinstance(file_paths, dict)

        if "prompt" in file_paths:
            assert os.path.exists(file_paths["prompt"])
            print(f"✓ Prompt file exists: {file_paths['prompt']}")

        if "testcase" in file_paths:
            assert isinstance(file_paths["testcase"], list)
            assert len(file_paths["testcase"]) > 0
            print(f"✓ Testcase files found: {len(file_paths['testcase'])} files")

    @pytest.mark.asyncio
    async def test_read_file_content(self, testcase_manager):
        """Test reading file content"""
        try:
            file_paths = testcase_manager.get_file_path("prompt")
            content = await testcase_manager.read_file_content(file_paths)
            assert isinstance(content, str)
            assert len(content) > 0
            print(
                f"✓ File content read successfully, length: {len(content)} characters"
            )
        except FileNotFoundError:
            print("⚠️  No prompt file found, skipping content read test")

    @pytest.mark.asyncio
    async def test_get_prompt_content(self, testcase_manager):
        """Test getting prompt content"""
        prompt_content = await testcase_manager.get_prompt_content()
        if prompt_content:
            assert isinstance(prompt_content, str)
            assert len(prompt_content) > 0
            print(
                f"✓ Prompt content retrieved, length: {len(prompt_content)} characters"
            )
            print(f"✓ Prompt content preview: {prompt_content[:150]}...")
        else:
            print("⚠️  No prompt content found")

    @pytest.mark.asyncio
    async def test_get_testcase_files(self, testcase_manager):
        """Test getting testcase files"""
        testcase_files = await testcase_manager.get_testcase_files()
        assert isinstance(testcase_files, list)

        if testcase_files:
            print(f"✓ Testcase files found: {len(testcase_files)} files")
            for file_info in testcase_files:
                assert "name" in file_info
                assert "path" in file_info
                assert "content" in file_info
                assert "vulnerability_schema" in file_info
                print(f"  - File: {file_info['name']}")
        else:
            print("⚠️  No testcase files found")

    @pytest.mark.asyncio
    async def test_load_data(self, testcase_manager):
        """Test loading all data"""
        print("\n【Step 2】Use load_data to load all data")

        try:
            all_data = await testcase_manager.load_data()
            print(f"✓ Data loading successful")

            # Validate vulnerability_schema
            assert "vulnerability_schema" in all_data
            schema = all_data["vulnerability_schema"]
            print(
                f"  - Vulnerability schema: {schema.primary_type}/{schema.secondary_type}/{schema.component_type}"
            )

            # Get prompt data
            if "prompt" in all_data:
                prompt_data = all_data["prompt"]
                assert "path" in prompt_data
                assert "content" in prompt_data
                print(f"  - Prompt path: {prompt_data['path']}")
                print(
                    f"  - Prompt content length: {len(prompt_data['content'])} characters"
                )
                print(f"  - Prompt content preview: {prompt_data['content'][:150]}...")
            else:
                print("  - No Prompt data found")

            # Get testcase data
            if "testcase" in all_data:
                testcase_data = all_data["testcase"]
                testcase_files = testcase_data["files"]
                assert "directory" in testcase_data
                assert isinstance(testcase_files, list)
                print(f"  - Testcase directory: {testcase_data['directory']}")
                print(f"  - Testcase file count: {len(testcase_files)}")

                return all_data  # Return data for subsequent tests
            else:
                print("  - No Testcase data found")
                return all_data

        except Exception as e:
            pytest.fail(f"✗ Data loading failed: {e}")

    @pytest.mark.asyncio
    async def test_validate_testcase_data_structure(self, testcase_manager):
        """Test validating testcase data structure"""
        print("\n【Step 3】Validate testcase data structure")

        all_data = await testcase_manager.load_data()

        if "testcase" in all_data:
            testcase_files = all_data["testcase"]["files"]

            for i, testcase in enumerate(testcase_files, 1):
                filename = testcase["name"]
                print(f"  - Testcase {i} ({filename}) data structure complete")

                # Check required fields
                required_fields = [
                    "name",
                    "path",
                    "content",
                    "formatted_content",
                    "instruction",
                    "block_location",
                ]
                missing_fields = [
                    field for field in required_fields if field not in testcase
                ]

                if missing_fields:
                    print(
                        f"✗ Test case {i} ({filename}) missing fields: {missing_fields}"
                    )
                    pytest.fail(f"Missing required fields: {missing_fields}")
                else:
                    print(f"✓ Test case {i} ({filename}) data structure complete")

                    # Validate formatting success
                    if "<fim_suffix>" in testcase["formatted_content"]:
                        print(f"  ✓ Formatting successful - contains FIM markers")
                    else:
                        print(f"  ⚠️  Formatting failed - no FIM markers found")

                    # Validate instruction loading
                    if testcase["instruction"]:
                        print(f"  ✓ Instruction loading successful")
                    else:
                        print(f"  ⚠️  No corresponding instruction found")

                    # Print block location details
                    if "block_location" in testcase and testcase["block_location"]:
                        block_loc = testcase["block_location"]
                        print(f"  === Block Location Details ===")
                        print(f"    Block start line: {block_loc.block_start_line}")
                        print(f"    Block start column: {block_loc.block_start_column}")
                        print(f"    Block end line: {block_loc.block_end_line}")
                        print(f"    Block end column: {block_loc.block_end_column}")
                        print(
                            f"    Block body start line: {block_loc.block_body_start_line}"
                        )
                        print(
                            f"    Block body start column: {block_loc.block_body_start_column}"
                        )
                        print(f"  =============================")
                    else:
                        print(f"  ⚠️  No block location found")

    @pytest.mark.asyncio
    async def test_instructions_loading(self, testcase_manager):
        """Test instructions loading functionality"""
        print("\n【Step 5】Test instructions loading")

        component_type = testcase_manager.vulnerability_schema.component_type

        # Test getting all instructions for component
        component_instructions = testcase_manager.get_instructions_for_component(
            component_type
        )
        assert isinstance(component_instructions, dict)
        print(
            f"✓ Loaded {len(component_instructions)} instructions for component: {component_type}"
        )

        # Test getting specific file instruction
        if component_instructions:
            # Get first instruction as test
            first_filename = list(component_instructions.keys())[0]
            instruction = testcase_manager.get_instruction(
                component_type, first_filename
            )
            assert instruction is not None
            print(f"✓ Instruction for {first_filename}: {instruction[:100]}...")

    @pytest.mark.asyncio
    async def test_content_formatting(self, testcase_manager):
        """Test content formatting functionality"""
        print("\n【Step 6】Test content formatting")

        testcase_files = await testcase_manager.get_testcase_files()

        if testcase_files:
            # Test formatting of first file
            first_file = testcase_files[0]
            file_path = first_file["path"]

            formatted_result = await testcase_manager.format_content(file_path)
            assert isinstance(formatted_result, tuple)
            assert len(formatted_result) == 2

            formatted_content, block_location = formatted_result
            assert isinstance(formatted_content, str)
            assert len(formatted_content) > 0

            # Print block location details
            print(f"=== Block Location for {first_file['name']} ===")
            print(f"Block start line: {block_location.block_start_line}")
            print(f"Block start column: {block_location.block_start_column}")
            print(f"Block end line: {block_location.block_end_line}")
            print(f"Block end column: {block_location.block_end_column}")
            print(f"Block body start line: {block_location.block_body_start_line}")
            print(f"Block body start column: {block_location.block_body_start_column}")
            print("==========================================")

            # Check if contains FIM markers
            if "<fim_suffix>" in formatted_content:
                print(f"✓ Content formatting successful for: {first_file['name']}")
            else:
                print(
                    f"⚠️  Content formatting may have issues for: {first_file['name']}"
                )

    @pytest.mark.asyncio
    async def test_get_testcase_data_complete(self, testcase_manager):
        """Test getting complete testcase data"""
        print("\n【Step 7】Test complete testcase data retrieval")

        testcase_data = await testcase_manager.get_testcase_data()
        assert isinstance(testcase_data, list)

        if testcase_data:
            print(f"✓ Retrieved {len(testcase_data)} complete testcase files")

            for file_info in testcase_data:
                required_fields = [
                    "name",
                    "path",
                    "content",
                    "formatted_content",
                    "instruction",
                    "vulnerability_schema",
                ]

                missing_fields = [
                    field for field in required_fields if field not in file_info
                ]
                if missing_fields:
                    pytest.fail(
                        f"Missing required fields in testcase data: {missing_fields}"
                    )

                print(f"  - File: {file_info['name']}")
                print(f"    Content length: {len(file_info['content'])}")
                print(
                    f"    Formatted content length: {len(file_info['formatted_content'])}"
                )
                print(f"    Has instruction: {file_info['instruction'] is not None}")

                # Print block location if available
                if "block_location" in file_info and file_info["block_location"]:
                    block_loc = file_info["block_location"]
                    print(f"    === Block Location ===")
                    print(f"      Block start line: {block_loc.block_start_line}")
                    print(f"      Block start column: {block_loc.block_start_column}")
                    print(f"      Block end line: {block_loc.block_end_line}")
                    print(f"      Block end column: {block_loc.block_end_column}")
                    print(
                        f"      Block body start line: {block_loc.block_body_start_line}"
                    )
                    print(
                        f"      Block body start column: {block_loc.block_body_start_column}"
                    )
                    print(f"    =====================")
                else:
                    print(f"    ⚠️  No block location available")
        else:
            print("⚠️  No complete testcase data found")


@pytest.mark.asyncio
async def test_full_workflow():
    """Test complete workflow"""
    print("\n=== TestcaseManager Business Logic Test ===")

    # Create vulnerability schema
    vulnerability_schema = VulnerabilitySchema(
        language="java",
        primary_type="XXE",
        secondary_type="XXE",
        component_type="XxeSaxBuilder",
        file_type={"type": "code"},
        sinks=[{"class_regex": "org.jdom2.input.SAXBuilder", "method_regex": "build"}],
    )

    # Create testcase manager
    manager = TestcaseManager(vulnerability_schema)

    # Test complete workflow
    try:
        all_data = await manager.load_data()
        assert "vulnerability_schema" in all_data

        if "testcase" in all_data:
            testcase_files = all_data["testcase"]["files"]
            assert len(testcase_files) > 0

            # Validate each testcase data structure
            for testcase in testcase_files:
                required_fields = [
                    "name",
                    "path",
                    "content",
                    "formatted_content",
                    "instruction",
                    "block_location",
                ]
                for field in required_fields:
                    assert field in testcase

                # Print block location for each testcase
                if "block_location" in testcase and testcase["block_location"]:
                    block_loc = testcase["block_location"]
                    print(f"=== Block Location for {testcase['name']} ===")
                    print(f"Block start line: {block_loc.block_start_line}")
                    print(f"Block start column: {block_loc.block_start_column}")
                    print(f"Block end line: {block_loc.block_end_line}")
                    print(f"Block end column: {block_loc.block_end_column}")
                    print(f"Block body start line: {block_loc.block_body_start_line}")
                    print(
                        f"Block body start column: {block_loc.block_body_start_column}"
                    )
                    print("==========================================")

        print("✓ Full workflow test completed successfully")

    except Exception as e:
        pytest.fail(f"Full workflow test failed: {e}")


if __name__ == "__main__":
    # Run all tests
    pytest.main([__file__, "-v", "-s"])
