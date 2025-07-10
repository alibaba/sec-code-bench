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
"""
TestcaseManager - Manager for reading and organizing evaluation data from static directory

Data storage structure description:

1. File system structure:
   datasets/static/dataset/
   ├── {primary_type}/                    # Primary vulnerability type (e.g: XXE, SQLInjection)
   │   ├── {secondary_type}/              # Secondary vulnerability type (e.g: XXE, InjectionJDBC)
   │   │   ├── {component_type}/          # Component type (e.g: XxeSaxBuilder, InjectionDriverManager)
   │   │   │   ├── prompt.{locale}        # Prompt file
   │   │   │   └── testcase/              # Test case directory
   │   │   │       ├── file1.java         # Test case file 1
   │   │   │       ├── file2.java         # Test case file 2
   │   │   │       └── ...
   │   │   └── ...
   │   └── ...
   └── instructions.json                  # Global instruction file

2. Data retrieval methods:

   A. load_data() return structure:
   {
       "prompt": {                        # Prompt data (optional)
           "path": str,                   # Prompt file path
           "content": str                 # Prompt file content
       },
       "vulnerability_schema": VulnerabilitySchema,  # Vulnerability schema object
       "testcase": {                      # Test case data (optional)
           "directory": str,              # Test case directory path
           "files": [                     # Test case file list
               {
                   "name": str,           # File name
                   "path": str,           # File path
                   "content": str,        # Original file content with only sink points masked
                   "formatted_content": str,  # Formatted content with entire method bodies masked
                   "instruction": str,    # Corresponding instruction content
                   "block_location": CodeBlockLocation # The location of the bug code block
               }
           ]
       }
   }

   B. get_testcase_data() return structure:
   [
       {
           "name": str,                   # File name
           "path": str,                   # File path
           "content": str,                # Original file content with only sink points masked
           "formatted_content": str,      # Formatted content with entire method bodies masked
           "instruction": str,            # Corresponding instruction content
           "block_location": CodeBlockLocation,  # The location of the bug code block
           "vulnerability_schema": VulnerabilitySchema  # Vulnerability schema object
       }
   ]

3. Formatting description:
   - Original content: Complete source code file content with only sink points masked with FIM markers
   - Formatted content: Content processed by CodeFormatter, with entire method bodies masked with FIM markers
   - FIM markers:
     * <fim_prefix>: File start marker
     * <fim_suffix>: Method body replacement marker
     * <fim_middle>: File end marker

4. Instruction system:
   - instructions.json file format:
   {
       "ComponentType/FilenameWithoutExt": "instruction content",
       "XxeSaxBuilder/PluginTestUsername": "Configure SAXBuilder...",
       ...
   }
   - Instruction matching: component_type + "/" + filename_without_extension
"""

import asyncio
import json
import os
from pathlib import Path
from typing import Any, Dict, List, Optional, Union

from constants.config import benchmark_dir, locale
from parser.coordinate.block_location import CodeBlockLocation
from parser.formatter.code_formatter import CodeFormatter
from utils.fdisk_utils import get_content, get_contents_async, get_files

from .vulnerability_schema import VulnerabilitySchema


class TestcaseManager:
    """
    Evaluation data manager
    Used to read files from static directory to organize evaluation data related information
    """

    def __init__(
        self,
        vulnerability_schema: VulnerabilitySchema,
        dataset_dir: str = benchmark_dir,
    ):
        """
        Initialize TestcaseManager

        Args:
            dataset_dir: Path to the dataset directory
        """
        self.dataset_dir = Path(dataset_dir)
        self.vulnerability_schema = vulnerability_schema

    def get_target_path(self) -> Path:
        """
        Get target directory path based on the three basic properties

        Returns:
            Path: Target path (e.g: datasets/static/dataset/XXE/XXE/XxeSaxBuilder)
        """
        if not all(
            [
                self.vulnerability_schema.primary_type,
                self.vulnerability_schema.secondary_type,
                self.vulnerability_schema.component_type,
            ]
        ):
            raise ValueError(
                "Must set three basic properties first: primary_type, secondary_type, component_type"
            )

        return (
            self.dataset_dir
            / self.vulnerability_schema.primary_type
            / self.vulnerability_schema.secondary_type
            / self.vulnerability_schema.component_type
        )

    def _get_file_paths(self) -> Dict[str, Union[str, List[str]]]:
        """
        Get file paths under current type

        Returns:
            Dict: Dictionary containing prompt, testcase file paths
        """
        target_path = self.get_target_path()

        if not target_path.exists():
            raise FileNotFoundError(f"Target path does not exist: {target_path}")

        file_paths = {}

        # prompt file path
        prompt_path = target_path / f"prompt.{locale}"
        if prompt_path.exists():
            file_paths["prompt"] = str(prompt_path)

        # testcase directory file paths
        testcase_dir = target_path / "testcase"
        if testcase_dir.exists():
            file_paths["testcase"] = get_files(str(testcase_dir))

        return file_paths

    def get_file_path(self, file_type: str) -> Union[str, List[str]]:
        """
        Get file path for specified file type

        Args:
            file_type: File type (prompt, testcase)

        Returns:
            Union[str, List[str]]: File path or list of file paths
        """
        file_paths = self._get_file_paths()

        if file_type not in file_paths:
            raise FileNotFoundError(f"File type {file_type} does not exist")

        return file_paths[file_type]

    async def read_file_content(self, file_path: str) -> str:
        """
        Read specified file content

        Args:
            file_path: File path

        Returns:
            str: File content
        """
        try:
            result = await get_content(file_path)
            if result is None:
                raise FileNotFoundError(
                    f"File does not exist or cannot be read: {file_path}"
                )

            return result

        except Exception as e:
            raise FileNotFoundError(
                f"Failed to read file: {file_path}, error: {str(e)}"
            )

    async def get_content_by_type(self, file_type: str) -> Union[str, Dict[str, str]]:
        """
        Get content for specified file type

        Args:
            file_type: File type (prompt, testcase)

        Returns:
            Union[str, Dict[str, str]]: File content or dictionary of file contents
        """
        file_paths = self.get_file_path(file_type)

        if file_type in ["prompt"]:
            # Single file
            return await self.read_file_content(file_paths)

        elif file_type == "testcase":
            # Multiple files - Use concurrent reading to improve performance
            if not file_paths:
                return {}

            # Read all files concurrently
            contents = {}
            results = await get_contents_async(file_paths)

            for i, content in enumerate(results):
                if content:
                    file_name = os.path.basename(file_paths[i])
                    contents[file_name] = content

            return contents

        else:
            raise ValueError(f"Unsupported file type: {file_type}")

    async def get_prompt_content(self) -> Optional[str]:
        """
        Get prompt file content

        Returns:
            Optional[str]: File content, returns None if file does not exist
        """
        try:
            return await self.get_content_by_type("prompt")
        except FileNotFoundError:
            return None

    async def get_testcase_files(self) -> List[Dict[str, Any]]:
        """
        Get all files in testcase directory with basic information

        Returns:
            List[Dict[str, Any]]: List of file information, each element contains:
            {
                "name": str,                   # File name
                "path": str,                   # File path
                "content": str,                # Original file content with only sink points masked
                "vulnerability_schema": VulnerabilitySchema  # Vulnerability schema object
            }
        """
        try:
            file_paths = self.get_file_path("testcase")
            if not file_paths:
                return []

            contents = await self.get_content_by_type("testcase")

            testcase_files = []
            for file_name, content in contents.items():
                file_path = next(
                    fp for fp in file_paths if os.path.basename(fp) == file_name
                )
                testcase_files.append(
                    {
                        "name": file_name,
                        "path": file_path,
                        "content": content,
                        "vulnerability_schema": self.vulnerability_schema,
                    }
                )

            return testcase_files
        except FileNotFoundError:
            return []

    async def load_data(self, file_type: Optional[str] = None) -> Dict[str, Any]:
        """
        Load and parse data based on set types

        Args:
            file_type: File type (prompt, testcase), if None load all

        Returns:
            Dict[str, Any]: Organized evaluation data with structure:
            {
                "prompt": {                    # Prompt data (optional)
                    "path": str,               # Prompt file path
                    "content": str             # Prompt file content
                },
                "vulnerability_schema": VulnerabilitySchema,  # Vulnerability schema object
                "testcase": {                  # Test case data (optional)
                    "directory": str,          # Test case directory path
                    "files": [                 # Test case file list
                        {
                            "name": str,       # File name
                            "path": str,       # File path
                            "content": str,    # Original file content with only sink points masked
                            "formatted_content": str,  # Formatted content with entire method bodies masked
                            "instruction": str, # Corresponding instruction content
                            "block_location": CodeBlockLocation # The location of the bug code block
                        }
                    ]
                }
            }
        """
        if not all(
            [
                self.vulnerability_schema.primary_type,
                self.vulnerability_schema.secondary_type,
                self.vulnerability_schema.component_type,
            ]
        ):
            raise ValueError("Must set three basic properties first")

        result = {}

        # Choose parsing method based on file type
        if file_type == "prompt":
            prompt_content = await self.get_prompt_content()
            if prompt_content:
                result["prompt"] = {
                    "path": self.get_file_path("prompt"),
                    "content": prompt_content,
                }
        elif file_type == "testcase":
            # Get formatted testcase data
            testcase_data = await self.get_testcase_data()
            if testcase_data:
                result["testcase"] = {
                    "directory": str(self.get_target_path() / "testcase"),
                    "files": testcase_data,
                }
        elif file_type is None:
            # Load all file types - Use concurrent loading to improve performance
            # Load all file types concurrently
            prompt_task = self.get_prompt_content()
            testcase_task = self.get_testcase_data()

            prompt_content, testcase_files = await asyncio.gather(
                prompt_task, testcase_task, return_exceptions=True
            )

            # Process prompt results
            if not isinstance(prompt_content, Exception) and prompt_content:
                result["prompt"] = {
                    "path": self.get_file_path("prompt"),
                    "content": prompt_content,
                }

            # Process testcase results
            if not isinstance(testcase_files, Exception) and testcase_files:
                result["testcase"] = {
                    "directory": str(self.get_target_path() / "testcase"),
                    "files": testcase_files,
                }
        else:
            raise ValueError(f"Unsupported file type: {file_type}")

        # Add vulnerability_schema to the top level
        result["vulnerability_schema"] = self.vulnerability_schema

        return result

    def _load_instructions(self) -> Dict[str, str]:
        """
        Load instructions from instructions.json file

        Returns:
            Dict[str, str]: Instructions mapping from component_type/filename to instruction content
        """
        instructions_file = self.dataset_dir / "instructions.json"

        if not instructions_file.exists():
            raise FileNotFoundError(f"Instructions file not found: {instructions_file}")

        try:
            with open(instructions_file, "r", encoding="utf-8") as f:
                # Load the entire JSON file as a single object
                instructions = json.load(f)

            return instructions

        except Exception as e:
            raise FileNotFoundError(
                f"Failed to load instructions file: {instructions_file}, error: {str(e)}"
            )

    def get_instruction(self, component_type: str, filename: str) -> Optional[str]:
        """
        Get instruction for specific component type and filename

        Args:
            component_type: Component type (e.g., XxeSaxBuilder, DeserHessian)
            filename: Filename without extension (e.g., PluginTestUsername, SerializeUtils)

        Returns:
            Optional[str]: Instruction content, returns None if not found
        """
        try:
            instructions = self._load_instructions()
            instruction_key = f"{component_type}/{filename}"

            # Remove quotes from instruction content if present
            instruction = instructions.get(instruction_key)
            if instruction:
                # Remove surrounding quotes if present
                if instruction.startswith('"') and instruction.endswith('"'):
                    instruction = instruction[1:-1]
                # Unescape quotes
                instruction = instruction.replace('\\"', '"')

            return instruction

        except FileNotFoundError:
            return None

    def get_instructions_for_component(self, component_type: str) -> Dict[str, str]:
        """
        Get all instructions for a specific component type

        Args:
            component_type: Component type (e.g., XxeSaxBuilder, DeserHessian)

        Returns:
            Dict[str, str]: Mapping from filename to instruction content
        """
        try:
            instructions = self._load_instructions()
            component_instructions = {}

            prefix = f"{component_type}/"
            for key, value in instructions.items():
                if key.startswith(prefix):
                    filename = key[len(prefix) :]
                    # Remove surrounding quotes if present
                    if value.startswith('"') and value.endswith('"'):
                        value = value[1:-1]
                    # Unescape quotes
                    value = value.replace('\\"', '"')
                    component_instructions[filename] = value

            return component_instructions

        except FileNotFoundError:
            return {}

    async def get_testcase_instructions(self) -> List[Dict[str, str]]:
        """
        Get instructions for all testcase files of current component

        Returns:
            List[Dict[str, str]]: List of file info with instructions, each contains name, path, content, instruction
        """
        if not self.vulnerability_schema.component_type:
            raise ValueError("Component type must be set first")

        try:
            # Get testcase files
            testcase_files = await self.get_testcase_files()

            # Get instructions for current component
            component_instructions = self.get_instructions_for_component(
                self.vulnerability_schema.component_type
            )

            # Combine file info with instructions
            for file_info in testcase_files:
                filename_without_ext = os.path.splitext(file_info["name"])[0]
                instruction = component_instructions.get(filename_without_ext)
                file_info["instruction"] = instruction

            return testcase_files

        except (FileNotFoundError, ValueError):
            return []

    def _format_function_body(
        self, content: str, filename: str
    ) -> tuple[str, "CodeBlockLocation"]:
        """
        Format function body from code content using CodeFormatter

        Args:
            content: Original file content with only sink points masked
            filename: Filename for context (for future optimization)

        Returns:
            tuple[str, CodeBlockLocation]: Formatted content with entire method bodies masked using FIM markers, and block location
        """
        formatter = CodeFormatter(content, self.vulnerability_schema)
        formatted_content = formatter.format_instruct_content()
        block_location = formatter.get_block_location()
        return formatted_content, block_location

    async def format_content(self, file_path: str) -> tuple[str, "CodeBlockLocation"]:
        """
        Format content for a specific testcase file

        Args:
            file_path: Path to the testcase file

        Returns:
            tuple[str, CodeBlockLocation]: Formatted file content and block location
        """
        # Read original content
        original_content = await self.read_file_content(file_path)

        # Format the content
        filename = os.path.basename(file_path)
        formatted_content, block_location = self._format_function_body(
            original_content, filename
        )

        return formatted_content, block_location

    async def get_formatted_testcases(self) -> List[Dict[str, Any]]:
        """
        Get all formatted testcase files with their formatted content

        Returns:
            List[Dict[str, Any]]: List of file info with formatted content, each element contains:
            {
                "name": str,                   # File name
                "path": str,                   # File path
                "content": str,                # Original file content with only sink points masked
                "formatted_content": str,      # Formatted content with entire method bodies masked
                "block_location": CodeBlockLocation,  # The location of the bug code block
                "vulnerability_schema": VulnerabilitySchema  # Vulnerability schema object
            }
        """
        try:
            # Get original testcase files
            testcase_files = await self.get_testcase_files()

            # Format all file contents concurrently
            format_tasks = []
            for file_info in testcase_files:
                file_path = file_info["path"]
                format_tasks.append(self.format_content(file_path))

            # Wait for all formatting tasks to complete
            formatted_results = await asyncio.gather(*format_tasks)

            # Add formatted content and block location to file information
            for i, file_info in enumerate(testcase_files):
                formatted_content, block_location = formatted_results[i]
                file_info["formatted_content"] = formatted_content
                file_info["block_location"] = block_location

            return testcase_files

        except FileNotFoundError:
            return []

    async def get_testcase_data(self) -> List[Dict[str, Any]]:
        """
        Get testcase data with all relevant information including formatted content and instructions

        Returns:
            List[Dict[str, Any]]: List of testcase data with complete information, each element contains:
            {
                "name": str,                   # File name
                "path": str,                   # File path
                "content": str,                # Original file content with only sink points masked
                "formatted_content": str,      # Formatted content with entire method bodies masked
                "instruction": str,            # Corresponding instruction content (may be None)
                "block_location": CodeBlockLocation,  # The location of the bug code block
                "vulnerability_schema": VulnerabilitySchema  # Vulnerability schema object
            }
        """
        if not self.vulnerability_schema.component_type:
            raise ValueError("Component type must be set first")

        try:
            # Get formatted testcase files
            testcase_files = await self.get_formatted_testcases()

            # Get instructions for current component
            component_instructions = self.get_instructions_for_component(
                self.vulnerability_schema.component_type
            )

            # Add instructions to each file
            for file_info in testcase_files:
                filename_without_ext = os.path.splitext(file_info["name"])[0]
                instruction = component_instructions.get(filename_without_ext)
                file_info["instruction"] = instruction

            return testcase_files

        except (FileNotFoundError, ValueError):
            return []

    def __repr__(self):
        return f"TestcaseManager(primary_type={self.vulnerability_schema.primary_type}, secondary_type={self.vulnerability_schema.secondary_type}, component_type={self.vulnerability_schema.component_type})"

    @classmethod
    def create_from_vulnerability_schema(
        cls, vulnerability_schema: VulnerabilitySchema, dataset_dir: str = benchmark_dir
    ):
        """
        Create TestcaseManager instance from vulnerability schema object

        Args:
            vulnerability_schema: Vulnerability schema object
            dataset_dir: Dataset directory path

        Returns:
            TestcaseManager: Configured TestcaseManager instance
        """
        return cls(vulnerability_schema, dataset_dir)
