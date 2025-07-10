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


class MybatisXmlParser(object):

    def __init__(self, content: str) -> None:
        self.content = content

    def get_fim_block_line(self) -> int:
        for line_index, line in enumerate(self.content.splitlines()):
            if line.find("<fim_suffix>") != -1:
                return line_index

        raise Exception(f"FIM block line not found, content: {self.content}")

    def parse_block_location(self, block_line: int) -> CodeBlockLocation:
        statments = ["select", "insert", "update", "delete", "sql"]
        lines = self.content.splitlines()
        if len(lines) < block_line:
            raise Exception(
                f"line number exceeds file length, xml: {len(lines)} block line number: {block_line}, content: {self.content}"
            )
        start_line, start_col, end_line, end_col = 0, 0, 0, 0
        target_statement = ""
        for line_index, line in enumerate(lines[block_line:]):
            flag = False
            for statment in statments:
                token = f"</{statment}>"
                col = line.find(token)
                if col != -1:
                    end_col = col + len(token)
                    end_line = block_line + line_index
                    target_statement = statment
                    flag = True
                    break
            if flag:
                break

        for idx in range(block_line):
            rv_idx = block_line - idx
            line = lines[rv_idx]
            token = f"<{target_statement} "
            col = line.find(token)
            if col != -1:
                start_col = col
                start_line = rv_idx
                break
        # for mybatis xml, the block body start line is the same as the start line
        return CodeBlockLocation(
            start_line, start_col, end_line, end_col, start_line, start_col
        )

    def parse_block_location_post_gen(
        self, block_previous_line: int
    ) -> CodeBlockLocation:
        """
        Parse the block location after code generation
        """
        return self.parse_block_location(block_previous_line)
