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

import asyncio
from typing import Any, Dict, List

from loguru import logger

import constants.config as conf
from constants.eval_type import EvalType
from evaluate.request_job import JobType
from parser.formatter.code_formatter import CodeFormatter
from parser.testcase.testcase_manager import TestcaseManager
from parser.testcase.vulnerability_schema import VulnerabilitySchema
from utils.fdisk_utils import get_contents_async, get_files, save_to_file_async


async def load_benchmark_dataset(
    benchmark_dir: str, bug_schemas: list[VulnerabilitySchema]
) -> list[dict]:
    [instruct_prompt, complete_prompt] = await get_contents_async(
        [conf.instruct_prompt_fpath, conf.complete_prompt_fpath]
    )
    all_testcases = []
    saving_tasks = []
    for bug_schema in bug_schemas:
        manager = TestcaseManager(bug_schema, benchmark_dir)
        testcases = await manager.load_data()
        if "testcase" not in testcases or "prompt" not in testcases:
            logger.error(
                f"No testcases or prompt found for {bug_schema}. Please check the file structure of benchmark dataset."
            )
            continue

        logger.info(
            f"Found {len(testcases['testcase']['files'])} testcases for {bug_schema}"
        )

        # supplement prompt info
        for testcase in testcases["testcase"]["files"]:
            instruct_prompt_content = instruct_prompt.replace(
                "{this_is_instruction}", testcase["instruction"]
            ).replace("{this_is_input_code}", testcase["formatted_content"])

            complete_prompt_content = complete_prompt.replace(
                "{this_is_input_code}", testcase["content"]
            )

            testcase["instruct_prompt"] = instruct_prompt_content
            testcase["complete_prompt"] = complete_prompt_content
            saving_tasks.append(
                save_to_file_async(
                    basedir=conf.cached_prompt_dir,
                    file_name=f"instruct_prompt_{testcase['name']}",
                    content=instruct_prompt_content,
                )
            )
            saving_tasks.append(
                save_to_file_async(
                    basedir=conf.cached_prompt_dir,
                    file_name=f"complete_prompt_{testcase['name']}",
                    content=complete_prompt_content,
                )
            )
        # add testcases to all_testcases
        all_testcases.append(testcases)

    await asyncio.gather(*saving_tasks)

    return all_testcases


async def load_eval_benchmark_dataset(
    benchmark: List[Dict[str, Any]],
    model: str,
    eval_type: EvalType,
) -> list[dict]:

    generation_files = [
        (fpath, idx, each_testcase["block_location"], dataset["vulnerability_schema"])
        for idx, dataset in enumerate(benchmark)
        for each_testcase in dataset["testcase"]["files"]
        for fpath in get_files(
            conf.cached_generated_model_dir_template.format(
                model=model,
                mode=(
                    JobType.COMPLETION_GEN
                    if eval_type == EvalType.COMPLETION_TYPE
                    else JobType.INSTRUCTION_GEN
                ),
                primary_type=dataset["vulnerability_schema"].primary_type,
                secondary_type=dataset["vulnerability_schema"].secondary_type,
                component_type=dataset["vulnerability_schema"].component_type,
                testcase_name=each_testcase["name"][: each_testcase["name"].rfind(".")],
            )
        )
    ]
    logger.debug(f"fpath: {generation_files[0][0]}, idx: {generation_files[0][1]}, block_location: {generation_files[0][2]}")
    generation_contents = await get_contents_async(
        [file_meta[0] for file_meta in generation_files]
    )
    eval_benchmark = []
    for i in range(len(generation_files)):
        org_one = benchmark[generation_files[i][1]]
        new_one = {}
        code_formatter = CodeFormatter(generation_contents[i], generation_files[i][3])
        block_content = code_formatter.format_llm_eval_content(generation_files[i][2])
        new_one["eval_prompt"] = org_one["prompt"]["content"].replace(
            "user_input_mask", block_content
        )
        new_one["vulnerability_schema"] = org_one["vulnerability_schema"]
        # Align with generated result, rather than the original testcases
        new_one["name"] = generation_files[i][0].split("/")[-2]

        eval_benchmark.append(new_one)

    return eval_benchmark
