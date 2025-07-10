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

import io
import os

import tree_sitter_java as tsjava
from tree_sitter import Language, Parser

from constants.config import file_marker, fim_prefix_marker
from parser.coordinate.block_location import CodeBlockLocation

JAVA_LANGUAGE = Language(tsjava.language())
parser = Parser(JAVA_LANGUAGE)
MAX_FILE_CONTENT = 1024 * 1024 * 10


class TreeSitterParser(object):

    def __init__(self, code: str) -> None:
        self._raw_content = code.encode("utf-8")
        self._is_binary = False
        self._allow_binary = False
        self.content = code

    def get_fim_block_line(self) -> int:
        for line_index, line in enumerate(self.content.splitlines()):
            if line.find("<fim_suffix>") != -1:
                return line_index

        raise Exception(f"FIM block line not found, content: {self.content}")

    def _replace_special_lines_with_empty(self, code: str) -> str:
        lines = code.splitlines()
        for i, line in enumerate(lines):
            if line.strip().startswith(file_marker) or line.strip().startswith(
                fim_prefix_marker
            ):
                lines[i] = ""  # Replace with empty line
        return "\n".join(lines)

    def parse_block_location(self, block_line: int) -> CodeBlockLocation:
        try:
            # Preprocess: replace special markers with empty lines to preserve line numbers
            code_for_parse = self._replace_special_lines_with_empty(self.content)
            tree = parser.parse(bytes(code_for_parse, "utf-8"))
        except Exception as e:
            raise Exception(f"Tree sitter parse failed, line: {block_line}, error: {e}")

        # find all methods in the code
        def find_all_methods(node):
            methods = []
            if node.type == "method_declaration":
                methods.append(node)
            elif node.type == "constructor_declaration":
                methods.append(node)
            for child in node.children:
                methods.extend(find_all_methods(child))
            return methods

        # get the root node
        root_node = tree.root_node

        # find all method declarations
        all_methods = find_all_methods(root_node)

        # print the location and text of each method
        for method in all_methods:
            start_line, start_col = method.start_point
            end_line, end_col = method.end_point
            if start_line <= block_line <= end_line:
                method_body = method.child_by_field_name("body")
                if method_body:
                    body_start_line, body_start_col = method_body.start_point
                    return CodeBlockLocation(
                        start_line,
                        start_col,
                        end_line,
                        end_col,
                        body_start_line,
                        body_start_col,
                    )
        return None

    def parse_block_location_post_gen(
        self, block_previous_line: int
    ) -> CodeBlockLocation:
        """
        Parse the block location after code generation
        """
        return self.parse_block_location(block_previous_line)

    def check_code_encoding(self, reader: io.BytesIO):
        # peek the first 1000 bytes, not forget to reset reader.
        header = reader.read(1000)
        reader.seek(0, os.SEEK_SET)

        if isinstance(header, str):
            # Read from string buffer, assume it is already a string.
            # Do not check encoding.
            data = reader.read(MAX_FILE_CONTENT)
            data_bytes = bytes(data, "utf-8", errors="replace")
            if len(data_bytes) >= MAX_FILE_CONTENT:
                self.error = "file too large"
                return
            self._raw_content = str(data_bytes, "utf-8", errors="replace")
            return

        # Check binary file format.
        if b"\0" in header:
            self._is_binary = True
            if not self._allow_binary:
                self.error = "ignored binary file"
                return
            else:
                self._raw_content = reader.read()
                return

        # Detect encoding from BOM.
        encoding = "UTF-8"
        # Check leading characters in header to detect BOOM
        bom_map = {
            b"\xef\xbb\xbf": "UTF-8",
            b"\xff\xfe": "UTF-16 LE",
            b"\xfe\xff": "UTF-16 BE",
            b"\xff\xfe\x00\x00": "UTF-32 LE",
            b"\x00\x00\xfe\xff": "UTF-32 BE",
        }
        for bom, bom_type in bom_map.items():
            if header.startswith(bom):
                encoding = bom_type
                break
        data = reader.read(MAX_FILE_CONTENT)
        if len(data) == MAX_FILE_CONTENT:
            self.error = "file too large"
            return
        self._raw_content = str(data, encoding, errors="replace")
