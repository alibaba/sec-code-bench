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

import argparse
import asyncio
import os
from contextlib import asynccontextmanager
from typing import Any, List, Optional, Type

from dotenv import load_dotenv

load_dotenv()

from loguru import logger

import constants.config as conf
from benchmark.handler import load_benchmark_dataset, load_eval_benchmark_dataset
from benchmark.statistics import do_statistics
from constants.eval_type import EvalType
from evaluate.base_consumer import BaseConsumerManager
from evaluate.consumer.generation_consumer import GenerationConsumerManager
from evaluate.consumer.static_eval_consumer import StaticEvalConsumer
from evaluate.request_job import GenerationJob, JobType, Stage, StaticEvalJob
from evaluate.resource_manager import ResourceManager
from parser.testcase.vulnerability_config import VulnerabilityConfig
from utils.rate_limiter import RateLimiter


def parse_arguments(args: Optional[List[str]] = None):
    """
    Create and configure the argument parser

    Returns:
        Configured ArgumentParser instance
    """
    parser = argparse.ArgumentParser(
        description="SAST SEC-LLM Coding Evaluation System",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
        Examples:
            # Quick start
            uv run sec_code_bench/eval_static.py --eval-type instruction
                
            # Custom evaluation with more configs
            uv run sec_code_bench/eval_static.py --eval-type instruction --models qwen3-235b-a22b,qwen-coder-plus --language java --vuln-file datasets/static/vulnerability_schema.yaml --cached-dir datasets/static/cached
        """,
    )

    # Required arguments
    parser.add_argument(
        "--models",
        dest="model_list",
        nargs="+",
        help="Comma-separated list of models to evaluate (e.g., GPT-4,CLAUDE-3), also can be set in .env file",
    )

    # Evaluation mode
    parser.add_argument(
        "--eval-type",
        choices=[mode.value for mode in EvalType],
        required=True,
        help=f"LLM SecurityEvaluation type",
    )

    # Language
    parser.add_argument(
        "--language", default="java", help="Language to evaluate (default: java)"
    )

    # Vulnerability types file
    parser.add_argument(
        "--vuln-file",
        "--eval-vulnerability-file",
        dest="vulnerability_file",
        default="datasets/static/vulnerability_schema.yaml",
        help="Path to YAML configuration file for vulnerability types to evaluate (default: datasets/static/vulnerability_schema.yaml)",
    )

    # Cached output directory configuration
    parser.add_argument(
        "--cached-dir",
        "--output-cached-dir",
        default="datasets/static/cached",
        help="Cached Output directory for results (default: datasets/static/cached)",
    )

    return parser.parse_args()


async def init_checker() -> tuple[dir, dir, Any]:
    # 0. Parse arguments
    parsed_args = parse_arguments()

    if parsed_args.cached_dir:
        conf.reset_cached_output_dir(parsed_args.cached_dir)

    # 0. Checker for llm client initialization
    models_for_gen = parsed_args.model_list or os.getenv(
        "MODEL_LIST", ""
    ).strip().split(",")
    if not models_for_gen:
        raise ValueError("MODEL_LIST is not set or empty")

    llms_config = {}
    for model in models_for_gen:
        model_api_key = os.environ.get(f"{model.strip().upper()}_LLM_API_KEY")
        model_api_endpoint = os.environ.get(f"{model.strip().upper()}_LLM_ENDPOINT")
        if not (model_api_key and model_api_endpoint):
            raise ValueError(
                f"Please set {model}_LLM_ENDPOINT and {model}_LLM_API_KEY of evaluated model in .env file before everything starts."
            )
        enable_thinking = bool(eval(os.environ.get(f"{model}_THINKING", "0")))
        enable_streaming = bool(eval(os.environ.get(f"{model}_STREAMING", "0")))

        llms_config[model] = {
            "api_key": model_api_key,
            "base_url": model_api_endpoint,
            "thinking": enable_thinking,
            "streaming": enable_streaming,
        }

    logger.info(f"LLMs set to be evaluated: {list(llms_config.keys())}")

    # 1. Checker for llms-as-judges
    llms_as_judge = os.environ.get("LLMS_AS_JUDGE")
    if not llms_as_judge:
        raise ValueError("LLMS_AS_JUDGE is not set")
    llms_as_judge = llms_as_judge.strip().split(",")
    if len(llms_as_judge) == 0:
        raise ValueError("LLMS_AS_JUDGE is empty")
    if len(llms_as_judge) % 2 == 0:
        raise ValueError("LLMS_AS_JUDGE must be an odd number")

    logger.info(f"LLMs used for static evaluation judge: {llms_as_judge}")

    if not (
        os.environ.get(f"LLMS_AS_JUDGE_API_KEY")
        and os.environ.get(f"LLMS_AS_JUDGE_ENDPOINT")
    ):
        raise ValueError(
            f"Please set LLMS_AS_JUDGE_ENDPOINT and LLMS_AS_JUDGE_API_KEY for static evaluation in .env file before everything starts."
        )

    # 2. Load benchmark dataset
    ## 2.1 Load vulnerability schemas
    config = VulnerabilityConfig(config_path=parsed_args.vulnerability_file)
    necessary_vuln_schemas = config.get_all_vulnerability_schemas(parsed_args.language)

    ## 2.2 Load necessary testcases
    benchmark_dir = conf.benchmark_dir
    if not benchmark_dir and not os.path.exists(benchmark_dir):
        raise ValueError("benchmark_dir is not set or not exists.")

    benchmark = await load_benchmark_dataset(
        os.path.join(benchmark_dir, str.lower(parsed_args.language)),
        bug_schemas=necessary_vuln_schemas,
    )
    return llms_config, benchmark, parsed_args


@asynccontextmanager
async def create_resource_managers(
    llms_config, consumer_manager_cls: Type[BaseConsumerManager]
):
    resource_managers = []
    for model, llm_config in llms_config.items():
        model_rm = ResourceManager(
            model_key=model,
            api_key=llm_config["api_key"],
            base_url=llm_config["base_url"],
            thread_count=conf.thread_count,
            rate_limiter=RateLimiter(
                max_cnts=conf.rate_limit_max_requests,
                window_seconds=conf.rate_limit_window_seconds,
            ),
            retry_count=conf.retry_count,
            enable_thinking=llm_config["thinking"],
            enable_streaming=llm_config["streaming"],
            consumer_manager_cls=consumer_manager_cls,
        )
        resource_managers.append(model_rm)
    try:
        yield resource_managers
    finally:
        for model_rm in resource_managers:
            model_rm.flush_queue()
            model_rm.shutdown()
        ResourceManager.reset()


async def construct_and_run_jobs(
    resource_managers: list[ResourceManager],
    benchmark: list[dict],
    stage: Stage,
    eval_type: EvalType,
    produce_rate: float = 0.5,
):

    async def _construct_job(model):
        if stage == Stage.GENERATION:
            datasets = [
                each_testcase
                for dataset in benchmark
                for each_testcase in dataset["testcase"]["files"]
            ]
        else:
            datasets = await load_eval_benchmark_dataset(benchmark, model, eval_type)

        for each_testcase in datasets:
            await asyncio.sleep(produce_rate)
            if stage == Stage.GENERATION:
                constructed_job = {
                    # basic properties
                    "job_type": (
                        JobType.COMPLETION_GEN
                        if eval_type == EvalType.COMPLETION_TYPE
                        else JobType.INSTRUCTION_GEN
                    ),
                    "vulnerability_schema": each_testcase["vulnerability_schema"],
                    "model": model,
                    # special properties
                    "generation_prompt": each_testcase["complete_prompt"],
                    "content": each_testcase["content"],
                    "path": each_testcase["path"],
                }
                job_clz = GenerationJob
            elif stage == Stage.EVALUATION:
                constructed_job = {
                    # basic properties
                    "job_type": JobType.LLM_EVAL,
                    "vulnerability_schema": each_testcase["vulnerability_schema"],
                    "model": model,
                    # special properties
                    "eval_prompt": each_testcase["eval_prompt"],
                    "llms_as_judge": os.environ.get("LLMS_AS_JUDGE", "")
                    .strip()
                    .split(","),
                    "name": each_testcase["name"],
                }
                job_clz = StaticEvalJob
            yield constructed_job, job_clz

    for each_model_rm in resource_managers:
        async for job_properties, job_clz in _construct_job(each_model_rm.model_key):
            each_model_rm.generate_and_add_job(job_properties, job_clz)


async def main():
    llms_config, benchmark, parsed_args = await init_checker()
    # 1. Start Generation Work
    logger.info("================== Start Generation ==================")
    async with create_resource_managers(
        llms_config, consumer_manager_cls=GenerationConsumerManager
    ) as gen_resource_managers:
        await construct_and_run_jobs(
            gen_resource_managers,
            benchmark,
            stage=Stage.GENERATION,
            eval_type=parsed_args.eval_type,
        )

    # 2. Start Static Evaluation Work
    logger.info("================== Start Evaluation ==================")
    async with create_resource_managers(
        llms_config, consumer_manager_cls=StaticEvalConsumer
    ) as eval_resource_managers:
        await construct_and_run_jobs(
            eval_resource_managers,
            benchmark,
            stage=Stage.EVALUATION,
            eval_type=parsed_args.eval_type,
        )

    # 3. Start Statistics
    logger.info("================== Start Statistics ==================")
    await do_statistics()


if __name__ == "__main__":
    logger.info(
        """
  ===========================
< LLM Security Evaluation Fwk! >
  ===========================
                           \\
                            \\
                              ^__^
                              (oo)\\_______
                              (__)\\       )\\/\\
                                  ||----w |
                                  ||     ||
                """
    )
    asyncio.run(main())
