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

from verifier.evaluator.base import EvaluationMethod, LanguageSupport
from verifier.llm.llm_base import LLMConfig
from verifier.llm.llm_manager import LLMManager
from verifier.llm.openai import OPENAI
from verifier.utils.testcase import Testcase, TestScenario

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
            if llm_manager.has_instance(model):
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
                scenarios=[TestScenario(s) for
                           s in case_data.get("scenarios")],
                params=case_data.get("params"),
                severity=case_data.get("severity"),
            )
            testcases_list.append(testcase)

        first_case = next(iter(data.values()), {})
        lang = first_case.get("language", "unknown")
        logger.info(f"total {len(testcases_list)} testcases: "
                    f"for language: {lang}")
    else:
        logger.error("Invalid benchmark file format")
        exit(-1)
    return testcases_list


