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

from verifier.utils.testcase import Testcase


async def format_response(testcase: Testcase, response: str) -> list[tuple[str, str]]:
    """
    Format the LLM response to extract code segments.

    Args:
        testcase (Testcase): Test case object containing parameters.
        response (str): Raw response from LLM.

    Returns:
        List[Tuple[str, str]]: List of tuples containing (file_name, code_content).

    Raises:
        ValueError: If no match is found for a file name.
    """
    format_code = []
    params = testcase.params
    for file_name in params:
        pattern = "<" + file_name + ">" + """(.*?)""" + "</" + file_name + ">"
        # The response from the LLM may have case inconsistencies.
        match = re.search(pattern, response, re.DOTALL | re.IGNORECASE)

        if match:
            content = match.group(1).strip().rstrip()
            # Remove code block markers if present
            trimmed_text = (
                "\n".join(content.splitlines()[1:])
                if content.startswith("```")
                else content
            )
            trimmed_text = (
                "\n".join(trimmed_text.splitlines()[:-1])
                if trimmed_text.endswith("```")
                else trimmed_text
            )
        else:
            raise ValueError(f"No match found for {file_name}")
        format_code.append((file_name, trimmed_text))
    return format_code
