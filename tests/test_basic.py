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
from loguru import logger

from sec_code_bench.utils.fdisk_utils import save_to_file_async


def test_limits_conditions():
    cnts = 23001
    max_step = 10000
    limits_condition = []
    for i in range(int(cnts / max_step) + 1):
        limits_condition.append(f" limit {i*max_step}, {max_step}")
    logger.info(f"limits_condition: {limits_condition}")

    assert len(limits_condition) == 3
    assert limits_condition[1] == f" limit {max_step}, {max_step}"


@pytest.mark.asyncio
async def test_save_file():
    await save_to_file_async(f"tests/", "my_test.txt", "123")
    assert os.path.exists(f"tests/my_test.txt")
    os.remove(f"tests/my_test.txt")
