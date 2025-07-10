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
import re
from concurrent.futures import ThreadPoolExecutor, as_completed

import pandas as pd
from loguru import logger

import constants.config as conf
import constants.result_meta as result_meta
from evaluate.base_consumer import BaseConsumerManager
from evaluate.request_job import JobType, StaticEvalJob
from utils.llm_utils import create_llm_client, invoke_llm


class StaticEvalConsumer(BaseConsumerManager):

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.risk_checking_pattern = re.compile(r"<risk>(\d+)</risk>", re.DOTALL)

    def _process_request_job(self, *, job: StaticEvalJob):
        formatted_result: pd.DataFrame = pd.DataFrame(columns=result_meta.columns)
        result_with_judge_model = self._llms_as_judge(job=job)
        sum_risk_score = 0
        for res, llm in result_with_judge_model:
            score = int(self.risk_checking_pattern.search(res).group(1))
            sum_risk_score += score
            new_row = pd.DataFrame(
                [[llm, score, res, job.eval_prompt]], columns=result_meta.columns
            )
            formatted_result = pd.concat([formatted_result, new_row], ignore_index=True)

        final_risk_score = round(sum_risk_score / len(result_with_judge_model))

        cached_generated_model_dir = conf.cached_generated_model_dir_template.format(
            model=job.model,
            mode=job.job_type,
            primary_type=job.vulnerability_schema.primary_type,
            secondary_type=job.vulnerability_schema.secondary_type,
            component_type=job.vulnerability_schema.component_type,
            testcase_name=job.name,
        )
        os.makedirs(cached_generated_model_dir, exist_ok=True)
        logger.info(
            f"Total risk score: {sum_risk_score} from {len(result_with_judge_model)} models"
        )

        final_row = pd.DataFrame(
            [
                [
                    result_meta.final_tag,
                    final_risk_score,
                    f"Average by {str(job.llms_as_judge)} models",
                    job.eval_prompt,
                ]
            ],
            columns=result_meta.columns,
        )
        formatted_result = pd.concat([formatted_result, final_row], ignore_index=True)

        formatted_result.to_csv(
            os.path.join(
                cached_generated_model_dir, conf.cached_judge_model_result_file_name
            ),
            index=False,
        )
        logger.info(f"Final risk score: {final_risk_score}")
        return final_risk_score

    def _validate_job(self, *, job: StaticEvalJob):
        return (
            job.job_type == JobType.LLM_EVAL
            and job.llms_as_judge is not None
            and len(job.llms_as_judge) % 2 == 1
        )

    def _llms_as_judge(self, *, job: StaticEvalJob):
        llms = job.llms_as_judge
        with ThreadPoolExecutor(max_workers=4) as executor:

            futures_to_judge_model = {
                executor.submit(
                    self._request_with_backoff,
                    invoke_llm,
                    job.eval_prompt,
                    llm,
                    create_llm_client(
                        os.environ.get("LLMS_AS_JUDGE_API_KEY"),
                        os.environ.get(f"LLMS_AS_JUDGE_ENDPOINT"),
                    ),
                    validator=lambda x: (
                        x.find("<risk>") != -1 and x.find("</risk>") != -1
                    ),
                ): llm
                for llm in llms
            }
        results = []
        for future in as_completed(futures_to_judge_model):
            llm = futures_to_judge_model[future]
            results.append((future.result(), llm))
        return results
