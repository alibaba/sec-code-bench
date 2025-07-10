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

from parser.coordinate.block_location import CodeBlockLocation


class PomParser(object):

    def __init__(self, content: str) -> None:
        self.content = content

    def get_fim_block_line(self) -> int:
        for line_index, line in enumerate(self.content.splitlines()):
            if line.find("<fim_suffix>") != -1:
                return line_index

        raise Exception(f"FIM block line not found, content: {self.content}")

    def parse_block_location(self, block_line: int) -> CodeBlockLocation:
        """
        Get the position coordinates of the specified dependency in pom.xml
        Perform text matching upward and downward from the given block_line to find the dependency boundaries

        Args:
            block_line: The line number of the starting search position

        Returns:
            CodeBlockLocation object containing coordinate information, returns None if parsing fails
        """
        if not self.content or not self.content.strip():
            return None

        try:
            # split the xml by line
            lines = self.content.split("\n")
            if block_line >= len(lines):
                return None

            # search for <dependency> tag upward from block_line
            start_line = -1
            start_col = -1
            for i in range(block_line, -1, -1):
                line = lines[i]
                dep_start_index = line.find("<dependency>")
                if dep_start_index != -1:
                    start_line = i
                    start_col = dep_start_index
                    break

            if start_line == -1:
                return None

            # search for </dependency> tag downward from block_line
            end_line = -1
            end_col = -1
            for i in range(block_line, len(lines)):
                line = lines[i]
                dep_end_index = line.find("</dependency>")
                if dep_end_index != -1:
                    end_line = i
                    end_col = dep_end_index + len("</dependency>")
                    break

            if end_line == -1:
                return None

            return CodeBlockLocation(
                block_start_line=start_line,
                block_start_column=start_col,
                block_end_line=end_line,
                block_end_column=end_col,
                block_body_start_line=start_line,
                block_body_start_column=start_col,
            )

        except Exception as e:
            raise RuntimeError(
                f"Failed to get dependency locations: {str(e)}, content: {self.content}, block_line: {block_line}"
            )

    def parse_block_location_post_gen(
        self, block_previous_line: int
    ) -> CodeBlockLocation:
        """
        Parse the block location after code generation
        """
        return self.parse_block_location(block_previous_line)
