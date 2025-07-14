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

from typing import List

from parser.coordinate.block_location import CodeBlockLocation
from parser.coordinate.code_parser import TreeSitterParser
from parser.coordinate.mybatis_parser import MybatisXmlParser
from parser.coordinate.pom_parser import PomParser
from parser.coordinate.property_paresr import PropertyParser
from parser.testcase.vulnerability_schema import VulnerabilitySchema


class CodeFormatter(object):
    def __init__(self, content: str, vulnerability_schema: VulnerabilitySchema) -> None:
        self.content = content
        self.vulnerability_schema = vulnerability_schema

    def get_block_location(self) -> CodeBlockLocation:
        if self.vulnerability_schema.is_mybatis_vulnerability():
            return self.get_mybatis_xml_block_location()
        elif self.vulnerability_schema.is_property_vulnerability():
            return self.get_property_file_block_location()
        elif self.vulnerability_schema.is_pom_vulnerability():
            return self.get_pom_dependency_block_location()
        else:
            return self.get_java_function_block_location()

    def get_block_location_post_gen(
        self, block_previous_line: int
    ) -> CodeBlockLocation:
        if self.vulnerability_schema.is_mybatis_vulnerability():
            return self.get_mybatis_xml_block_location_post_gen(block_previous_line)
        elif self.vulnerability_schema.is_property_vulnerability():
            return self.get_property_file_block_location_post_gen(block_previous_line)
        elif self.vulnerability_schema.is_pom_vulnerability():
            return self.get_pom_dependency_block_location_post_gen(block_previous_line)
        else:
            return self.get_java_function_block_location_post_gen(block_previous_line)

    def get_mybatis_xml_block_location(self) -> CodeBlockLocation:
        mybatis_parser = MybatisXmlParser(self.content)
        return mybatis_parser.parse_block_location(mybatis_parser.get_fim_block_line())

    def get_mybatis_xml_block_location_post_gen(
        self, block_previous_line: int
    ) -> CodeBlockLocation:
        mybatis_parser = MybatisXmlParser(self.content)
        return mybatis_parser.parse_block_location_post_gen(block_previous_line)

    def get_property_file_block_location_post_gen(
        self, block_previous_line: int
    ) -> CodeBlockLocation:
        property_parser = PropertyParser(self.content)
        return property_parser.parse_block_location_post_gen(block_previous_line)

    def get_pom_dependency_block_location_post_gen(
        self, block_previous_line: int
    ) -> CodeBlockLocation:
        pom_parser = PomParser(self.content)
        return pom_parser.parse_block_location_post_gen(block_previous_line)

    def get_java_function_block_location_post_gen(
        self, block_previous_line: int
    ) -> CodeBlockLocation:
        tree_sitter_parser = TreeSitterParser(self.content)
        return tree_sitter_parser.parse_block_location_post_gen(block_previous_line)

    def get_property_file_block_location(self) -> CodeBlockLocation:
        property_parser = PropertyParser(self.content)
        return property_parser.parse_block_location()

    def get_pom_dependency_block_location(self) -> CodeBlockLocation:
        pom_parser = PomParser(self.content)
        return pom_parser.parse_block_location(pom_parser.get_fim_block_line())

    def get_java_function_block_location(self) -> CodeBlockLocation:
        tree_sitter_parser = TreeSitterParser(self.content)
        return tree_sitter_parser.parse_block_location(
            tree_sitter_parser.get_fim_block_line()
        )

    def format_llm_eval_content(self, code_block_location: CodeBlockLocation) -> str:
        """
        Format the content with fim_suffix markers to indicate the vulnerability location.
        Converts lines within the specified range to <fim_suffix> markers.
        """
        block_content = self.extract_block_content_post_gen(code_block_location.block_start_line + 1)
        return block_content

    def format_instruct_content(self) -> str:
        """
        Format the content with fim_suffix markers to indicate the vulnerability location.
        Converts lines within the specified range to <fim_suffix> markers.
        """
        block_location = self.get_block_location()
        if not block_location:
            raise Exception(f"fail to parse block location, content: {self.content}")

        lines = self.content.split("\n")
        return self._extract_code_context(lines, block_location)

    def _validate_column_range(self, line: str, column: int, column_type: str) -> None:
        """
        Validate column range validity.

        Args:
            line: The line to validate
            column: Column index to validate
            column_type: Type of column (start/end) for error message
        """
        if column < 0 or column > len(line):
            error_msg = (
                f"Invalid {column_type} column: {column}, line length: {len(line)}"
            )
            raise ValueError(error_msg)

    def extract_block_content_post_gen(self, block_previous_line: int) -> str:
        """
        Extract the code within the block based on block location coordinates.
        """
        lines = self.content.split("\n")
        block_location = self.get_block_location_post_gen(block_previous_line - 1)
        return self._extract_code_within_block(lines, block_location)

    def _extract_code_within_block(
        self, lines: List[str], block_location: CodeBlockLocation
    ) -> str:
        """
        Extract the code within the block based on block location coordinates.

        Args:
            lines: List of lines from the content
            block_location: CodeBlockLocation object with range information

        Returns:
            String containing the extracted code within the specified block
        """
        if not block_location:
            raise ValueError("block_location cannot be None")

        # Get the block coordinates
        start_line = block_location.block_start_line
        start_column = block_location.block_start_column
        end_line = block_location.block_end_line
        end_column = block_location.block_end_column

        # Validate line numbers
        if start_line < 0 or end_line >= len(lines):
            raise ValueError(
                f"Invalid line range: start={start_line}, end={end_line}, total_lines={len(lines)}"
            )

        if start_line > end_line:
            raise ValueError(
                f"Start line {start_line} cannot be greater than end line {end_line}"
            )

        # Extract the code
        if start_line == end_line:
            # Single line block
            line = lines[start_line]
            self._validate_column_range(line, start_column, "start")
            self._validate_column_range(line, end_column, "end")

            if start_column > end_column:
                raise ValueError(
                    f"Start column {start_column} cannot be greater than end column {end_column}"
                )

            # Extract the substring from start_column to end_column
            return line[start_column:end_column]
        else:
            # Multi-line block
            extracted_lines = []

            # First line: from start_column to end of line
            first_line = lines[start_line]
            self._validate_column_range(first_line, start_column, "start")
            extracted_lines.append(first_line[start_column:])

            # Middle lines: entire lines
            for i in range(start_line + 1, end_line):
                extracted_lines.append(lines[i])

            # Last line: from beginning to end_column
            last_line = lines[end_line]
            self._validate_column_range(last_line, end_column, "end")
            extracted_lines.append(last_line[:end_column])

            return "\n".join(extracted_lines)

    def _extract_code_context(
        self, lines: List[str], block_location: CodeBlockLocation
    ) -> str:
        """
        Replace code in lines that matches the range with <fim_suffix> marker.
        Keep the method declaration but replace the method body content.

        Args:
            lines: List of lines from the content
            block_location: CodeBlockLocation object with range information

        Returns:
            String with the method body replaced by <fim_suffix>
        """
        result = []

        # Use method declaration start position, not method body start position
        method_start_line = block_location.block_start_line
        method_body_start_line = block_location.block_body_start_line
        method_body_start_column = block_location.block_body_start_column
        method_end_line = block_location.block_end_line
        method_end_column = block_location.block_end_column

        # Add all lines before the method
        for i in range(method_start_line):
            if i < len(lines):
                result.append(lines[i])

        # Add method declaration lines (from method start to method body start)
        for i in range(method_start_line, method_body_start_line):
            if i < len(lines):
                result.append(lines[i])

        # Handle method body replacement
        if method_body_start_line == method_end_line:
            # Single line method body
            if method_body_start_line < len(lines):
                line = lines[method_body_start_line]
                self._validate_column_range(line, method_body_start_column, "start")
                self._validate_column_range(line, method_end_column, "end")

                # Keep the opening brace, add fim_suffix, keep the closing brace
                formatted_line = (
                    line[: method_body_start_column + 1]
                    + "<fim_suffix>"
                    + line[method_end_column - 1 :]
                )
                result.append(formatted_line)
        else:
            # Multi-line method body
            # Handle method body start line (keep the opening brace)
            if method_body_start_line < len(lines):
                start_line_content = lines[method_body_start_line]
                self._validate_column_range(
                    start_line_content, method_body_start_column, "start"
                )
                result.append(
                    start_line_content[: method_body_start_column + 1] + "<fim_suffix>"
                )

            # Skip middle lines (they are replaced by fim_suffix)

            # Handle method end line (keep the closing brace)
            if method_end_line < len(lines):
                end_line_content = lines[method_end_line]
                self._validate_column_range(end_line_content, method_end_column, "end")
                result.append(end_line_content[method_end_column - 1 :])

        # Add all lines after the method
        for i in range(method_end_line + 1, len(lines)):
            result.append(lines[i])

        # Join with newlines and remove the last extra newline if present
        formatted_result = "\n".join(result)
        if formatted_result.endswith("\n"):
            formatted_result = formatted_result[:-1]

        return formatted_result
