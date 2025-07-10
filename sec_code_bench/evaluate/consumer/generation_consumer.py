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

from loguru import logger

import constants.config as conf
from checker.checker_factory import CheckerFactory
from evaluate.base_consumer import BaseConsumerManager
from evaluate.request_job import GenerationJob
from utils.fdisk_utils import save_to_file
from utils.llm_utils import invoke_llm
from utils.mark_utils import remove_markdown_code_block, strip_fim_marks


class GenerationConsumerManager(BaseConsumerManager):

    def _process_request_job(self, *, job: GenerationJob):
        logger.info(f"Processing job: {job.vulnerability_schema} {job.path}")
        generate_retries = 3
        while generate_retries > 0:
            res = self._request_with_backoff(
                invoke_llm,
                job.generation_prompt,
                job.model,
                self._llm_client,
                enable_thinking=self._enable_thinking,
                streaming=self._enable_streaming,
            )

            final_res = job.content.replace(conf.fim_marker, res)
            final_res = strip_fim_marks(final_res)
            final_res = remove_markdown_code_block(final_res)

            # Only perform checks if checking flag is True
            if CheckerFactory.is_language_supported(job.vulnerability_schema.language):
                checker = CheckerFactory.get_checker(job.vulnerability_schema.language)
                if conf.enable_syntax_check and not checker.check_syntax_from_content(
                    final_res
                ):
                    logger.warning(f"Checker failed for {job.path} due to syntax error")
                    generate_retries -= 1
                    continue
                if conf.enable_sink_validation and not checker.check_sink_in(
                    code_content=final_res,
                    vulnerability_schema=job.vulnerability_schema,
                ):
                    logger.warning(f"Checker failed for {job.path} due to sink error")
                    generate_retries -= 1
                    continue
            else:
                raise ValueError(
                    f"Unsupported checker for language: {job.vulnerability_schema.language}"
                )

            # If checking flag is False, skip all checks and proceed directly
            cached_generated_model_dir = (
                conf.cached_generated_model_dir_template.format(
                    model=job.model,
                    mode=job.job_type,
                    primary_type=job.vulnerability_schema.primary_type,
                    secondary_type=job.vulnerability_schema.secondary_type,
                    component_type=job.vulnerability_schema.component_type,
                    testcase_name=job.path[
                        job.path.rfind("/") + 1 : job.path.rfind(".")
                    ],
                )
            )
            os.makedirs(cached_generated_model_dir, exist_ok=True)
            save_to_file(
                cached_generated_model_dir,
                conf.cached_model_result_file_name,
                final_res,
            )
            return
        logger.warning(
            f"Failed to generate valid testcase for {job.path} due to checker error"
        )

    def _validate_job(self, *, job: GenerationJob) -> bool:
        if job.content.find(conf.fim_marker) == -1:
            raise ValueError(
                f"fim_marker is not found in content of testcase: {job.path}"
            )
        return True
