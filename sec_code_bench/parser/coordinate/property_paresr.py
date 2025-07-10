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


class PropertyParser(object):
    def __init__(self, content: str) -> None:
        self.content = content

    def get_fim_block_line(self) -> int:
        for line_index, line in enumerate(self.content.splitlines()):
            if line.find("<fim_suffix>") != -1:
                return line_index

        raise Exception(f"FIM block line not found, content: {self.content}")

    def parse_block_location(self) -> CodeBlockLocation:
        """
        Parse the block location before code generation
        """
        block_line = self.get_fim_block_line()
        totalLines = len(self.content.split("\n"))
        startLine = max(0, block_line - 1)
        endLine = min(totalLines - 1, block_line + 1)
        return CodeBlockLocation(startLine, 0, endLine, 0, startLine, 0)

    def parse_block_location_post_gen(
        self, block_previous_line: int
    ) -> CodeBlockLocation:
        """
        Parse the block location after code generation
        Args:
            block_previous_line: the line number of the previous block
        Returns:
            CodeBlockLocation: the block location
        """
        totalLines = len(self.content.split("\n"))
        startLine = max(0, block_previous_line - 10)
        endLine = min(totalLines - 1, block_previous_line + 10)
        return CodeBlockLocation(startLine, 0, endLine, 0, startLine, 0)
