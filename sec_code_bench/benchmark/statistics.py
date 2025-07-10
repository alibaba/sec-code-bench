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
from io import StringIO
from typing import Optional

import pandas as pd
from loguru import logger

import constants.config as conf
import constants.result_meta as result_meta
from utils.fdisk_utils import get_all_files, get_contents_async


async def do_statistics(
    cached_generated_dir: Optional[str] = conf.cached_generated_dir,
):
    cached_dir = cached_generated_dir
    if not os.path.exists(cached_dir):
        raise ValueError(f"Cached directory {cached_dir} does not exist")

    # 1. Load cached judging results
    judging_files = get_all_files(
        cached_dir, suffix=conf.cached_judge_model_result_file_name
    )
    if not judging_files:
        logger.error(
            f"No judging results {conf.cached_judge_model_result_file_name} found in {cached_dir}"
        )
        return

    # 2. Load cached generated results and collect data
    model_to_res = {}
    judging_contents = await get_contents_async(judging_files)
    for judging_file, judging_content in zip(judging_files, judging_contents):
        model, primary_type = _extract_meta_from_judging_path(
            judging_file.replace(cached_dir, "")
        )

        judging_df = pd.read_csv(StringIO(judging_content))
        testcase_score = judging_df[judging_df["judge_llm"] == result_meta.final_tag][
            result_meta.score_col
        ].values[0]
        if model not in model_to_res:
            model_to_res[model] = {}
        if primary_type not in model_to_res[model]:
            model_to_res[model][primary_type] = {
                "testcase_count": 0,
                "all_risk_score": 0,
            }
        model_to_res[model][primary_type]["testcase_count"] += 1
        model_to_res[model][primary_type]["all_risk_score"] += testcase_score

    # 3. Calculate statistics
    statistics_rows = []
    for model, primary_types in model_to_res.items():
        for primary_type, scores in primary_types.items():
            testcase_count = scores["testcase_count"]
            avg_risk_score = scores["all_risk_score"] / testcase_count
            statistics_rows.append(
                [model, primary_type, testcase_count, avg_risk_score]
            )

    statistics_pd = pd.DataFrame(
        statistics_rows, columns=result_meta.statistics_columns
    )

    # 4. Save statistics to file
    statistics_pd.to_csv(
        os.path.join(cached_dir, conf.cached_statistics_file_name), index=False
    )
    logger.info(
        f"Final LLM Security Risk Statistics saved to {os.path.join(cached_dir, conf.cached_statistics_file_name)}"
    )


def _extract_meta_from_judging_path(sub_fpath: str):
    items = sub_fpath.split("/")
    model = items[1]
    mode = items[2]
    primary_type = items[3]
    secondary_type = items[4]
    component_type = items[5]
    testcase_name = items[6]
    return model, primary_type
