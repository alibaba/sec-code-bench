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
import json
import logging
from datetime import datetime
from pathlib import Path
from typing import Any

from sec_code_bench.evaluator.base import EvaluationMethod, LanguageSupport
from sec_code_bench.llm.llm_base import LLMConfig
from sec_code_bench.llm.llm_manager import LLMManager
from sec_code_bench.llm.openai import OPENAI
from sec_code_bench.statistic.pass_at_k_statistic import (
    calculate_final_score,
    calculate_scenario_score,
)
from sec_code_bench.utils.config_loader import ConfigLoader
from sec_code_bench.utils.fdisk_utils import save_file
from sec_code_bench.utils.logger_utils import Logger
from sec_code_bench.utils.testcase import Testcase, TestScenario

# Define specification format and example
SPECIFICATION_FORMAT = "PROVIDER::MODEL::API_KEY::BASE_URL"
EXAMPLE_SPECIFICATION = "OPENAI::model-name::your-api-key::https://api.openai.com/v1"
__version__ = "1.0.0"
__banner__ = """
                        +---------------------------------------------+
                        |              SecCodeBench                   |
                        |  - Security Evaluation Framework for LLM-   |
                        |              generated code                 |
                        +---------------------------------------------+
"""


def basic_parser() -> argparse.ArgumentParser:
    """
    Parse command line arguments.
    Used for public startup args and some mandatory args

    Returns:
        Parsed arguments object.
    """

    parser = argparse.ArgumentParser(
        description=(
            "SecCodeBench - Security Evaluation Framework for LLM-generated code"
        )
    )

    parser.add_argument(
        "--benchmark",
        required=True,
        help="Path to the benchmark test file",
        default="./datasets/benchmark/java.json",
    )

    parser.add_argument(
        "--config",
        type=str,
        default="config.ini",
        help="Configuration file path (default: config.ini)",
    )

    parser.add_argument(
        "--log_level",
        type=str,
        default="INFO",
        choices=["DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"],
        help="Logging level (default: INFO)",
    )

    parser.add_argument(
        "--log-dir",
        type=str,
        default="./logs/",
        help="Log directory path (default: ./logs/)",
    )

    parser.add_argument(
        "--language_list",
        required=True,
        help="Benchmark languages, e.g., java, python",
        nargs="+",
        type=str.lower,
    )

    parser.add_argument(
        "--judge_llm_list",
        required=False,
        help=(
            f"Judge LLMs provided as {SPECIFICATION_FORMAT}, "
            f"e.g., {EXAMPLE_SPECIFICATION}. "
            "Can be specified multiple times. Must be odd number for majority voting."
        ),
        nargs="+",
    )

    parser.add_argument(
        "--experiment_cycle",
        type=int,
        default=10,
        help="Number of experiment cycles for each test case (default: 10)",
    )

    return parser


def basic_checker(args: argparse.Namespace, parser: argparse.ArgumentParser) -> None:
    """
    Check command line arguments.

    Args:
        args: Parsed arguments object.
        parser: Argument parser object.

    Returns:
        None
    """

    if args.judge_llm_list is None:
        print(
            "Warning: --judge_llm_list is not set. This may cause issues with evaluation."
        )
    elif len(args.judge_llm_list) % 2 == 0:
        parser.error(
            "The number of judge LLMs must be odd to "
            "ensure a majority vote can be reached."
        )


def basic_init_log(
    args: argparse.Namespace, model: str
) -> tuple[Path, Path, logging.Logger]:
    """
    Initialize basic information including logging and directories.

    Args:
        args: Parsed arguments object.
        model: Model name.

    Returns:
        Tuple containing work directory, result directory, and logger object.
    """
    start_time_format = datetime.now().strftime("%Y-%m-%d_%H-%M")

    work_dir = Path(args.log_dir) / "worker" / __version__ / model / start_time_format
    result_dir = Path(args.log_dir) / "results" / __version__ / model / start_time_format

    log_path = (
        Path(args.log_dir)
        / "results"
        / __version__
        / model
        / start_time_format
        / f"{model}-{start_time_format}.log"
    )

    # if work_dir.exists():
    #     shutil.rmtree(work_dir)
    work_dir.mkdir(parents=True, exist_ok=True)

    Logger.initialize(log_path, args.log_level)

    logger = Logger.get_logger(__name__)

    print(__banner__)

    return Path(work_dir), Path(result_dir), logger


def basic_load_config(args: argparse.Namespace) -> ConfigLoader:
    """
    Load configuration from file.

    Args:
        args: Parsed arguments object.

    Returns:
        Configuration loader object.
    """
    # Read configuration file
    validation_rules = {
        "BASE": {
            "max_concurrent_files": {
                "type": int,
                "required": False,
                "default": 100,
            },
            "testcase_batch_size": {
                "type": int,
                "required": False,
                "default": 50,
            },
            "locale": {"type": str, "required": False, "default": "zh-CN"},
        }
    }
    config = ConfigLoader(validation_rules)
    config.load(args.config)

    return config


def basic_init_llm(args: argparse.Namespace, logger: logging.Logger) -> LLMManager:
    """
    Initialize LLM instances.

    Args:
        args: Parsed arguments object.
        logger: Logger object.

    Returns:
        LLM manager object.
    """
    # Display LLM list to be tested
    logger.info("================== Model Registration ==================")
    # Parse and initialize evaluation LLM
    llm_manager = LLMManager()

    all_llm_specs = []

    # Process eval_llm_list if it exists and is not empty
    if hasattr(args, "eval_llm_list") and args.eval_llm_list:
        all_llm_specs.extend(args.eval_llm_list)

    # Process judge_llm_list if it exists and is not empty, and merge into the result
    if hasattr(args, "judge_llm_list") and args.judge_llm_list:
        all_llm_specs.extend(args.judge_llm_list)

    # Remove duplicates to ensure uniqueness
    all_llm_specs = list(set(all_llm_specs))

    for spec in all_llm_specs:
        parts = spec.split("::")
        if len(parts) != 4:
            logger.error(f"Invalid LLM format: {spec}")
            raise ValueError(f"Invalid LLM format: {spec}")
        provider, model, api_key, base_url = parts
        config = LLMConfig(model=model, url=base_url, api_key=api_key)
        # Initialize based on provider
        try:
            # Check if already exists in manager
            if llm_manager.get_instance(model):
                logger.warning(f"Model: {model} already registered")
                continue

            logger.info(f"Registering model {model}...")
            if provider.upper() == "OPENAI":
                llm_manager.register_model_type(model, OPENAI)
                extra_body = {}
                # qwen3 needs to explicitly disable thinking in non-streaming mode
                if model == "qwen3-235b-a22b":
                    extra_body = {"enable_thinking": False}
                llm_manager.create_instance(model, config, extra_body=extra_body)

            else:
                raise ValueError(f"Unsupported LLM provider: {provider}")
        except Exception as e:
            logger.error(f"Failed to initialize LLM {spec}: {e}")
            raise
    return llm_manager


def basic_init_testcase(
    args: argparse.Namespace, logger: logging.Logger
) -> list[Testcase]:
    """
    Initialize test cases from benchmark file.

    Args:
        args: Parsed arguments object.
        logger: Logger object.

    Returns:
        List of test case objects.
    """
    # Load test cases
    try:
        with open(args.benchmark, encoding="utf-8") as f:
            data = json.load(f)
        logger.info(f"Loaded {len(data)} test cases")
    except Exception as e:
        logger.error(f"Failed to load benchmark file: {str(e)}")
        raise

    testcases_list: list[Testcase] = []
    if isinstance(data, dict):
        for case_id, case_data in data.items():
            testcase = Testcase(
                case_id=case_id,
                FuncTester=EvaluationMethod(case_data.get("FuncTester")),
                SecTester=EvaluationMethod(case_data.get("SecTester")),
                language=LanguageSupport(case_data.get("language")),
                prompt=case_data.get("prompt"),
                template=case_data.get("template"),
                scenarios=[TestScenario(s) for s in case_data.get("scenarios")],
                params=case_data.get("params"),
                severity=case_data.get("severity"),
            )
            testcases_list.append(testcase)
        logger.info(
            f"total {len(testcases_list)} testcases for language: "
            f"{case_data.get('language') if data else 'unknown'}"
        )
    else:
        logger.error("Invalid benchmark file format")
        exit(-1)
    return testcases_list


def basic_calc_score(
    pass_at_1_result: list[Any], result_dir: Path, logger: logging.Logger
) -> None:
    """
    Calculate and save scores.

    Args:
        pass_at_1_result: List of test results.
        result_dir: Directory to save results.
        logger: Logger object.

    Returns:
        None
    """
    final_score1 = calculate_final_score(
        pass_at_1_result,
        category_weights={
            "low": 1.0,
            "medium": 2.0,
            "high": 4.0,
        },
        scenario_weights={
            "gen": 4.0,
            "gen-hints": 1.0,
            "fix": 4.0,
            "fix-hints": 1.0,
        },
    )
    logger.info(f"Final Score pass@1: {final_score1}")
    scenario_score_list = calculate_scenario_score(
        pass_at_1_result,
        category_weights={
            "low": 1.0,
            "medium": 2.0,
            "high": 4.0,
        },
    )

    score_result: dict[str, Any] = {
        "scenario": scenario_score_list,
        "total": final_score1,
    }

    file_path = result_dir / "score.json"
    save_file(
        file_path,
        json.dumps(score_result, indent=2, ensure_ascii=False),
        True,
    )

    # Provide completion marker for WEB to check output data
    save_file(
        result_dir / "finish",
        "",
        True,
    )
