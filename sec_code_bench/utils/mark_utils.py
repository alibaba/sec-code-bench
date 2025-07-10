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

import re


def strip_fim_marks(input_str):
    """
    Remove FIM (Fill-In-the-Middle) marks from input string.

    Args:
        input_str (str): The input string containing FIM marks

    Returns:
        str: The string with FIM marks removed

    Raises:
        RuntimeError: If the input format is invalid
    """
    # Remove content between <filename> and <fim_prefix> markers
    filename_index = input_str.find("<filename>")
    fim_prefix_index = input_str.find("<fim_prefix>")

    if (
        filename_index == -1
        or fim_prefix_index == -1
        or fim_prefix_index < filename_index
    ):
        raise RuntimeError(f"Encounter error when strip_fim_marks: {input_str}")

    input_str = input_str[:filename_index] + input_str[fim_prefix_index:]

    # Remove all FIM markers
    input_str = input_str.replace("<filename>", "")
    input_str = input_str.replace("<fim_prefix>", "")
    input_str = input_str.replace("<fim_middle>", "")
    input_str = input_str.replace("<fim_suffix>", "")
    input_str = input_str.replace("<|cursor|>", "")

    return input_str


def remove_markdown_code_block(input_str):
    """
    Removes Markdown code block formatting from the LLM response.

    Args:
        input_str (str): The input text containing Markdown code blocks

    Returns:
        str: The cleaned text without Markdown formatting
    """
    if not input_str:
        return input_str

    # Define regex to match Markdown code block tags
    regex = r"```[a-zA-Z]*\n|```"

    # Replace matched content using regex
    return re.sub(regex, "", input_str).strip()


