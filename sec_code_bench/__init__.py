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
from zoneinfo import ZoneInfo
from pathlib import Path
from typing import Any, Literal

from sec_code_bench.config.language_config import LanguageConfig
from sec_code_bench.config.system_config import SystemConfig
from sec_code_bench.editor import Editor, EditorFactory, IDEType
from sec_code_bench.evaluator.base import EvaluationMethod, LanguageSupport
from sec_code_bench.llm.llm_base import LLMConfig
from sec_code_bench.llm.llm_manager import LLMManager
from sec_code_bench.llm.openai import OPENAI
from sec_code_bench.llm.openai_responses import OpenAIResponses
from sec_code_bench.llm.anthropic_messages import AnthropicMessages
from sec_code_bench.statistic.pass_at_k_statistic import (
    calculate_final_score,
    calculate_scenario_score,
)
from sec_code_bench.utils.config_loader import ConfigLoader
from sec_code_bench.utils.fdisk_utils import save_file
from sec_code_bench.utils.logger_utils import Logger
from sec_code_bench.utils.rate_limiter import RateLimiter
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
        "--eval-llm",
        required=True,
        help=(
            f"LLM to benchmark provided as {SPECIFICATION_FORMAT}, "
            f"e.g., {EXAMPLE_SPECIFICATION}."
        ),
    )

    parser.add_argument(
        "--parameters",
        type=str,
        default=None,
        help=(
            "Optional JSON string of parameters to pass to LLM API calls. "
            "e.g., '{\"enable_thinking\": true}'. "
            "These parameters will be merged into the API request body."
        ),
    )

    parser.add_argument(
        "--rpm-limit",
        type=int,
        default=None,
        help=(
            "Optional RPM (Requests Per Minute) limit "
            "for the evaluated LLM models. "
            "If not specified, defaults to 60 RPM. "
            "This controls the rate limiting for API calls "
            "to prevent exceeding provider limits."
        ),
    )

    # multi-language config option (repeatable)
    parser.add_argument(
        "--lang-config",
        action="append",
        dest="lang_configs",
        metavar="LANG:LOCALE:BENCHMARK",
        help=(
            "Per-language configuration in format"
            " 'language:locale:benchmark_path'. "
            "Can be specified multiple times for multi-language evaluation. "
            "Example: "
            "--lang-config cpp:en-US:./datasets/benchmark/cpp/cpp.json"
        ),
    )

    parser.add_argument(
        "--log-level",
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
        "--judge-llm-list",
        required=False,
        help=(
            f"Judge LLMs provided as {SPECIFICATION_FORMAT}, "
            f"e.g., {EXAMPLE_SPECIFICATION}. "
            "Can be specified multiple times. Must be odd number for majority voting."
        ),
        nargs="+",
    )

    parser.add_argument(
        "--experiment-cycle",
        type=int,
        default=10,
        help="Number of experiment cycles for each test case (default: 10)",
    )

    parser.add_argument(
        "--batch-size",
        type=int,
        default=15,
        help="Batch size for processing test cases (default: 15)",
    )

    return parser


def e2e_parser() -> argparse.ArgumentParser:
    """
    Parse command line arguments for e2e.py (Editor-based evaluation).
    
    This parser is used for IDE/CLI editor-based code generation evaluation,
    where code is generated by external tools like Claude Code, Cursor, etc.

    Returns:
        Parsed arguments object.
    """
    parser = argparse.ArgumentParser(
        description=(
            "SecCodeBench E2E - Security Evaluation for IDE/CLI Code Generators"
        )
    )

    parser.add_argument(
        "--editor",
        "-e",
        choices=[e.value for e in IDEType],
        type=str,
        required=True,
        help="Specify the editor type to be used (e.g., claude-code, cursor)",
    )

    parser.add_argument(
        "--prepare",
        "-f",
        action="store_true",
        default=False,
        help="Call the prepare method of the editor before execution",
    )

    parser.add_argument(
        "--threads",
        type=int,
        default=1,
        help="Number of worker threads for parallel execution (default: 1)",
    )

    parser.add_argument(
        "--debug",
        action="store_true",
        default=False,
        help=(
            "Enable debug mode for application type editors - "
            "save debug snapshots on exceptions"
        ),
    )

    parser.add_argument(
        "--prompt",
        "-p",
        type=str,
        default="",
        help=(
            "Filter testcases: use range like '0-4' for indices "
            "or string for exact/partial key matching. Empty means all testcases."
        ),
    )

    # multi-language config option (repeatable)
    parser.add_argument(
        "--lang-config",
        action="append",
        dest="lang_configs",
        metavar="LANG:LOCALE:BENCHMARK",
        help=(
            "Per-language configuration in format"
            " 'language:locale:benchmark_path'. "
            "Can be specified multiple times for multi-language evaluation. "
            "Example: "
            "--lang-config java:en-US:./datasets/benchmark/java/java.json"
        ),
    )

    parser.add_argument(
        "--log-level",
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
        "--judge-llm-list",
        required=False,
        help=(
            f"Judge LLMs provided as {SPECIFICATION_FORMAT}, "
            f"e.g., {EXAMPLE_SPECIFICATION}. "
            "Can be specified multiple times. Must be odd number for majority voting."
        ),
        nargs="+",
    )

    parser.add_argument(
        "--experiment-cycle",
        type=int,
        default=10,
        help="Number of experiment cycles for each test case (default: 10)",
    )

    parser.add_argument(
        "--batch-size",
        type=int,
        default=15,
        help="Batch size for processing test cases (default: 15)",
    )

    return parser


def e2e_checker(args: argparse.Namespace, parser: argparse.ArgumentParser) -> None:
    """
    Check command line arguments for e2e.py.

    Args:
        args: Parsed arguments object.
        parser: Argument parser object.

    Returns:
        None
    """
    # Load system configuration
    try:
        system_config = SystemConfig.load()
    except FileNotFoundError as e:
        parser.error(f"System configuration error: {e}")

    # Validate language configuration
    has_lang_configs = args.lang_configs is not None and len(args.lang_configs) > 0

    if not has_lang_configs:
        parser.error(
            "Must specify language configuration: use --lang-config "
            "(e.g., --lang-config java:en-US:./benchmark.json). "
        )

    # Extract evaluating languages from lang_configs
    evaluating_languages = set()

    for config_str in args.lang_configs:
        # Parse language from config string (format: language:locale:path)
        language = config_str.split(":")[0].lower()
        evaluating_languages.add(language)

    # Check if any evaluating language requires LLM judges
    languages_need_llm = system_config.get_languages_need_llm_judges()
    languages_requiring_judges = evaluating_languages.intersection(
        set(lang.lower() for lang in languages_need_llm)
    )

    if languages_requiring_judges:
        # Check if judge_llm_list is provided
        if args.judge_llm_list is None or len(args.judge_llm_list) == 0:
            lang_list = ", ".join(sorted(languages_requiring_judges))
            parser.error(
                f"The following language(s) require LLM judges: {lang_list}. "
                f"Please provide --judge-llm-list argument with an odd number of judge LLMs."
            )
        # Check if the number is odd
        if len(args.judge_llm_list) % 2 == 0:
            parser.error(
                "The number of judge LLMs must be odd to "
                "ensure a majority vote can be reached."
            )


def basic_checker(args: argparse.Namespace, parser: argparse.ArgumentParser) -> None:
    """
    Check command line arguments.

    Args:
        args: Parsed arguments object.
        parser: Argument parser object.

    Returns:
        None
    """

    # Load system configuration
    try:
        system_config = SystemConfig.load()
    except FileNotFoundError as e:
        parser.error(f"System configuration error: {e}")

    # Validate language configuration
    has_lang_configs = args.lang_configs is not None and len(args.lang_configs) > 0

    if not has_lang_configs:
        parser.error(
            "Must specify language configuration: use --lang-config "
            "(e.g., --lang-config cpp:en-US:./benchmark.json). "
        )

    # Extract evaluating languages from lang_configs
    evaluating_languages = set()

    for config_str in args.lang_configs:
        # Parse language from config string (format: language:locale:path or language:path)
        language = config_str.split(":")[0].lower()
        evaluating_languages.add(language)

    # Check if any evaluating language requires LLM judges
    languages_need_llm = system_config.get_languages_need_llm_judges()
    languages_requiring_judges = evaluating_languages.intersection(
        set(lang.lower() for lang in languages_need_llm)
    )

    if languages_requiring_judges:
        # Check if judge_llm_list is provided
        if args.judge_llm_list is None or len(args.judge_llm_list) == 0:
            lang_list = ", ".join(sorted(languages_requiring_judges))
            parser.error(
                f"The following language(s) require LLM judges: {lang_list}. "
                f"Please provide --judge_llm_list argument with an odd number of judge LLMs."
            )
        # Check if the number is odd
        if len(args.judge_llm_list) % 2 == 0:
            parser.error(
                "The number of judge LLMs must be odd to "
                "ensure a majority vote can be reached."
            )


def parse_language_configs(args: argparse.Namespace) -> list[LanguageConfig]:
    """
    Parse language configurations from command line arguments.

    Args:
        args: Parsed arguments object.

    Returns:
        List of LanguageConfig objects.

    Raises:
        ValueError: If configuration is invalid.
    """
    lang_configs: list[LanguageConfig] = []

    if args.lang_configs:
        for config_str in args.lang_configs:
            lang_config = LanguageConfig.from_string(config_str)
            lang_configs.append(lang_config)
        return lang_configs

    # This should not be reached if basic_checker is called first
    raise ValueError("Must specify --lang-config")


def basic_init_log(
    args: argparse.Namespace, model: str, version: str = __version__
) -> tuple[Path, logging.Logger]:
    """
    Initialize basic information including logging and directories.

    Args:
        args: Parsed arguments object.
        model: Model name.

    Returns:
        Tuple containing work directory, result directory, and logger object.
    """
    start_time = datetime.now(ZoneInfo("Asia/Shanghai"))
    start_day_format = start_time.strftime("%Y-%m-%d")
    start_time_format = start_time.strftime("%Y-%m-%d_%H-%M")

    result_dir = (
        Path(args.log_dir)
        / "results"
        / version
        / model
        / start_day_format
        / start_time_format
    )

    log_path = (
        Path(args.log_dir)
        / "results"
        / version
        / model
        / start_day_format
        / start_time_format
        / f"{model}-{start_time_format}.log"
    )

    Logger.initialize(log_path, args.log_level)

    logger = Logger.get_logger(__name__)

    print(__banner__)

    return Path(result_dir), logger


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
            "testcase_batch_size": {
                "type": int,
                "required": False,
                "default": 15,
            },
            "locale": {
                "type": str,
                "required": False,
                "default": "zh-CN",
            },
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

    # Process eval_llm FIRST to ensure it takes priority in registration
    # (when eval_llm and a judge_llm share the same model name but different
    # providers, the eval_llm's provider must win since it's looked up by
    # model name during evaluation)
    if hasattr(args, "eval_llm") and args.eval_llm:
        all_llm_specs.append(args.eval_llm)

    # Process judge_llm_list if it exists and is not empty, and merge into the result
    if hasattr(args, "judge_llm_list") and args.judge_llm_list:
        all_llm_specs.extend(args.judge_llm_list)

    # Remove duplicates while preserving order (eval_llm first).
    # Using dict.fromkeys preserves insertion order and deduplicates.
    # This is critical because set() has non-deterministic ordering which
    # can cause the wrong provider to register when the same model name
    # appears in both eval_llm and judge_llms with different providers.
    all_llm_specs = list(dict.fromkeys(all_llm_specs))

    # Get RPM limit from args if provided, otherwise use default
    rpm_limit = getattr(args, "rpm_limit", None)
    if rpm_limit is not None:
        if rpm_limit <= 0:
            logger.error(f"Invalid RPM limit: {rpm_limit}. Must be a positive integer.")
            raise ValueError(
                f"Invalid RPM limit: {rpm_limit}. Must be a positive integer."
            )
        logger.info(f"Using custom RPM limit: {rpm_limit} requests per minute")
    else:
        logger.info("Using default RPM limit: 60 requests per minute")

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
            if llm_manager.has_instance(model):
                logger.warning(f"Model: {model} already registered")
                continue

            logger.info(f"Registering model {model}...")

            # Create rate limiter if RPM limit is specified
            rate_limiter = None
            if rpm_limit is not None:
                rate_limiter = RateLimiter(
                    max_cnts=rpm_limit, window_seconds=60, burst_size=1
                )
                logger.info(f"Created rate limiter for {model}: {rpm_limit} RPM")

            if provider.upper() == "OPENAI":
                llm_manager.register_model_type(model, OPENAI)
                llm_manager.create_instance(
                    name=model, config=config, rate_limit=rate_limiter
                )
            elif provider.upper() == "OPENAI_RESPONSES":
                llm_manager.register_model_type(model, OpenAIResponses)
                extra_body = {}
                llm_manager.create_instance(
                    name=model,
                    config=config,
                    rate_limit=rate_limiter,
                    extra_body=extra_body,
                )
            elif provider.upper() == "ANTHROPIC":
                llm_manager.register_model_type(model, AnthropicMessages)
                llm_manager.create_instance(
                    name=model, config=config, rate_limit=rate_limiter
                )
            else:
                raise ValueError(f"Unsupported LLM provider: {provider}")
        except Exception as e:
            logger.error(f"Failed to initialize LLM {spec}: {e}")
            raise
    return llm_manager


def basic_calc_score(
    pass_at_1_result: list[Any],
    result_dir: Path,
    logger: logging.Logger,
    system_config: SystemConfig,
) -> None:
    """
    Calculate and save scores.

    Args:
        pass_at_1_result: List of test results.
        result_dir: Directory to save results.
        logger: Logger object.
        system_config: System configuration object.

    Returns:
        None
    """
    # Get weights from system configuration
    category_weights = system_config.get_category_weights()
    scenario_weights = system_config.get_scenario_weights()

    final_score1 = calculate_final_score(
        pass_at_1_result,
        category_weights=category_weights,
        scenario_weights=scenario_weights,
    )
    logger.info(f"Final Score pass@1: {final_score1}")
    scenario_score_list = calculate_scenario_score(
        pass_at_1_result,
        category_weights=category_weights,
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

    # Temporary record of scores before weighting (all weights set to 1.0)
    origin_category_weights = {k: 1.0 for k in category_weights.keys()}
    origin_scenario_weights = {k: 1.0 for k in scenario_weights.keys()}

    origin_final_score1 = calculate_final_score(
        pass_at_1_result,
        category_weights=origin_category_weights,
        scenario_weights=origin_scenario_weights,
    )
    origin_scenario_score_list = calculate_scenario_score(
        pass_at_1_result,
        category_weights=origin_category_weights,
    )

    origin_score_result: dict[str, Any] = {
        "scenario": origin_scenario_score_list,
        "total": origin_final_score1,
    }

    origin_file_path = result_dir / "origin_score.json"
    save_file(
        origin_file_path,
        json.dumps(origin_score_result, indent=2, ensure_ascii=False),
        True,
    )

    # Calculate and save per-language scores
    languages = set(
        record.get("language") for record in pass_at_1_result if record.get("language")
    )
    for lang in languages:
        lang_records = [r for r in pass_at_1_result if r.get("language") == lang]
        if not lang_records:
            continue

        lang_final_score = calculate_final_score(
            lang_records,
            category_weights=category_weights,
            scenario_weights=scenario_weights,
        )
        lang_scenario_score_list = calculate_scenario_score(
            lang_records,
            category_weights=category_weights,
        )
        lang_score_result: dict[str, Any] = {
            "scenario": lang_scenario_score_list,
            "total": lang_final_score,
        }
        lang_file_path = result_dir / f"{lang}_score.json"
        save_file(
            lang_file_path,
            json.dumps(lang_score_result, indent=2, ensure_ascii=False),
            True,
        )
        logger.info(f"Language {lang} Score: {lang_final_score}")

    # Provide completion marker for WEB to check output data
    save_file(
        result_dir / "finish",
        "",
        True,
    )


def detect_benchmark_format(data: dict | list) -> Literal["local", "remote"]:
    """
    Detect the format of a benchmark JSON file.

    Args:
        data: The loaded JSON data from the benchmark file.

    Returns:
        Format type string: "remote" or "local".
    """
    # Remote format: dict with "verify_urls" in testcase entries
    if isinstance(data, dict) and len(data) > 0:
        first_item = next(iter(data.values()))
        if (
            isinstance(first_item, dict)
            and "verify_urls" in first_item
            and "remote_prompt_paths" in first_item
        ):
            return "remote"

    return "local"


def init_remote_testcases(
    data: dict[str, Any],
    language: str,
    logger: logging.Logger,
) -> list[Testcase]:
    """
    Initialize test cases from a remote-format benchmark file.

    The remote format is a dict (like local format) but includes:
    - verify_urls: dict mapping scenarios to verification API URLs
    - remote_prompt_paths: dict mapping scenarios to remote prompt paths

    This format is produced by the preprocess_remote_benchmark.py script.

    Args:
        data: The loaded JSON data (already parsed dict).
        language: The programming language for these testcases.
        logger: Logger object.

    Returns:
        List of Testcase objects configured for remote verification.
    """
    testcases_list: list[Testcase] = []

    for case_id, case_data in data.items():
        # Parse scenarios
        scenario_strs = case_data.get("scenarios", [])
        scenarios = [TestScenario(s) for s in scenario_strs]

        # Parse verify_urls dict (scenario string -> URL)
        verify_urls_raw = case_data.get("verify_urls", {})
        verify_urls: dict[TestScenario, str] = {}
        for scenario_str, url in verify_urls_raw.items():
            try:
                scenario = TestScenario(scenario_str)
                verify_urls[scenario] = url
            except ValueError:
                logger.warning(f"Unknown scenario in verify_urls: {scenario_str}")

        # Parse remote_prompt_paths dict (scenario string -> path)
        remote_paths_raw = case_data.get("remote_prompt_paths", {})
        remote_prompt_paths: dict[TestScenario, str] = {}
        for scenario_str, path in remote_paths_raw.items():
            try:
                scenario = TestScenario(scenario_str)
                remote_prompt_paths[scenario] = path
            except ValueError:
                logger.warning(f"Unknown scenario in remote_prompt_paths: {scenario_str}")

        # Get locale from testcase data
        try:
            locale = case_data.get("locale")
        except ValueError as e:
            logger.warning(f"Invalid locale: {case_data.get('locale')}")
            raise e

        testcase = Testcase(
            case_id=case_id,
            is_remote=True,
            prompts={},  # Will be loaded from prompt files
            verify_urls=verify_urls,
            remote_prompt_paths=remote_prompt_paths,
            scenarios=scenarios,
            locale=locale,
            language=LanguageSupport(case_data.get("language", language)),
            template=case_data.get("template", ""),
            prompt=case_data.get("prompt", case_id),
            FuncTester=EvaluationMethod(case_data.get("FuncTester")),
            SecTester=EvaluationMethod(case_data.get("SecTester")),
            severity=case_data.get("severity"),
            params=case_data.get("params", {}),
        )
        testcases_list.append(testcase)

    logger.info(f"Loaded {len(testcases_list)} remote testcases for {language}")

    return testcases_list


def init_testcases_auto_detect(
    benchmark_path: Path,
    language: str,
    logger: logging.Logger,
) -> list[Testcase]:
    """
    Auto-detect benchmark format and initialize testcases accordingly.

    Supports two formats:
    - "remote": Remote format (dict with verify_urls and remote_prompt_paths)
    - "local": Local format (dict without remote verification fields)

    Args:
        benchmark_path: Path to the benchmark JSON file.
        language: The programming language for these testcases.
        logger: Logger object.

    Returns:
        List of Testcase objects.
    """
    try:
        with open(benchmark_path, encoding="utf-8") as f:
            data = json.load(f)
    except Exception as e:
        logger.error(f"Failed to load benchmark file {benchmark_path}: {str(e)}")
        raise

    format_type = detect_benchmark_format(data)
    logger.info(f"Detected benchmark format: {format_type}")

    if format_type == "remote":
        testcases = init_remote_testcases(data, language, logger)
        return testcases
    else:
        # raise exception
        raise ValueError("Unsupported benchmark format")
