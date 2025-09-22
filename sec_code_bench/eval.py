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

from __future__ import annotations

import argparse
import asyncio
import copy
import os
import re
import shutil
import traceback
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from typing import Any

from sec_code_bench import (
    EXAMPLE_SPECIFICATION,
    SPECIFICATION_FORMAT,
    basic_calc_score,
    basic_checker,
    basic_init_llm,
    basic_init_log,
    basic_init_testcase,
    basic_load_config,
    basic_parser,
)
from sec_code_bench.evaluator.base import (
    EvaluatorResult,
    SyntaxCheckError,
)
from sec_code_bench.llm.llm_base import LLMBase
from sec_code_bench.llm.llm_manager import LLMManager
from sec_code_bench.security_monitor import SecurityMonitor
from sec_code_bench.statistic.pass_at_k_statistic import (
    stat_pass_at_k_score,
)
from sec_code_bench.statistic.statistic_manager import do_statistic
from sec_code_bench.tester.function import FunctionTester
from sec_code_bench.tester.security import SecurityTester
from sec_code_bench.utils.fdisk_utils import (
    save_test_results,
    write_file_async,
)
from sec_code_bench.utils.logger_utils import Logger
from sec_code_bench.utils.testcase import Testcase, TestScenario

# Global logger object
LOG: Logger | None = None


def parse_and_check_args() -> argparse.Namespace:
    """
    Parse command line arguments.

    Returns:
        argparse.Namespace: Parsed arguments object.
    """
    parser = basic_parser()

    parser.add_argument(
        "--eval_llm_list",
        required=True,
        help=(
            f"LLM to benchmark provided as {SPECIFICATION_FORMAT}, "
            f"e.g., {EXAMPLE_SPECIFICATION}. "
            "Can be specified multiple times to test multiple LLMs."
        ),
        nargs="+",
    )

    args = parser.parse_args()

    basic_checker(args, parser)

    return args


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


async def run_once(
    args: argparse.Namespace,
    cycle: int,
    testcase: Testcase,
    llm: LLMBase,
    work_dir: Path,
    scenario: TestScenario,
    executor: ThreadPoolExecutor,
    llm_manager: LLMManager,
) -> None:
    """
    Handle one test case under one scenario.

    Args:
        args: Command line arguments.
        cycle (int): Current experiment cycle.
        testcase (Testcase): Test case to handle.
        llm (LLMBase): LLM instance to use.
        work_dir (Path): Working directory for temporary files.
        scenario (TestScenario): Scenario to test.
        executor (ThreadPoolExecutor): Thread pool executor.
        llm_manager (LLMManager): Manager for LLM instances.
    """
    # Get LLM response, ensure it's a string result
    try:
        prompt = testcase.get_scenario_prompt(scenario)
        response = await llm.aquery(prompt)
    except Exception as e:
        LOG.error(f"LLM query failed for {testcase.template}: {e}", exc_info=True)
        testcase.set_error_result(
            cycle, scenario, f"LLM query failed for {testcase.template}: {e}"
        )
        return

    if not isinstance(response, str):
        LOG.error(
            f"LLM returned non-string response for {testcase.template}: {type(response)}"
        )
        testcase.set_error_result(
            cycle,
            scenario,
            f"LLM returned non-string response for {testcase.template}: {type(response)}",
        )
        return

    # Format response to ensure it's executable code
    try:
        code_list = await format_response(testcase, response)
    except Exception as e:
        LOG.error(f"Failed to format response: {str(e)}")
        testcase.set_error_result(
            cycle,
            scenario,
            f"Failed to format response: {str(e)} \n response: \n{response}",
        )
        return
    if code_list is None:
        LOG.error("Failed to format response after 3 attempts")
        testcase.set_error_result(
            cycle, scenario, "Failed to format response after 3 attempts"
        )
        return

    # Set up code template directory and temporary directory,
    # ensure successful file output
    current_dir = Path(__file__).parent.absolute()
    code_template_dir = (
        current_dir.parent
        / "datasets/templates"
        / testcase.language.value
        / testcase.template
    )

    code_dir = work_dir / f"{testcase.prompt}_{scenario.value}_cycle-{cycle}"
    testcase.set_code_paths(cycle, scenario, code_dir)

    try:
        shutil.copytree(str(code_template_dir), str(code_dir))
        for file_name, code in code_list:
            file_path = testcase.params.get(file_name)
            if file_path:
                full_path = code_dir / file_path
                full_path.parent.mkdir(parents=True, exist_ok=True)
                await write_file_async(full_path, "w", "utf-8", code)
    except Exception as e:
        LOG.error(f"Error setting up test work dir: {str(e)}")
        # # Clean up failed directory
        # if code_dir.exists():
        #     shutil.rmtree(str(code_dir), ignore_errors=True)
        testcase.set_fun_results(
            cycle, scenario, f"Error setting up test work dir: {str(e)}"
        )
        return

    try:
        fun_result = await FunctionTester.function_eval_with_retry(
            testcase, code_dir, llm_manager, llm, executor, args.judge_llm_list
        )
        short_result = copy.deepcopy(fun_result)
        short_result.stdout = "see log for details" if short_result.stdout else ""
        short_result.stderr = "see log for details" if short_result.stderr else ""
        LOG.info(
            f"Function evaluation result for {testcase.case_id}:"
            f"{scenario.value} is {short_result}"
        )
    except SyntaxCheckError as e:
        LOG.error(f"Syntax error in {code_dir}")
        testcase.set_error_result(
            cycle,
            scenario,
            f"Syntax error in {code_dir} \n {str(e)}",
        )
        return
    except Exception as e:
        LOG.error(f"Error running function test: {str(e)}")
        testcase.set_error_result(
            cycle,
            scenario,
            f"Functional check failed; security check was not performed.\n "
            f"Functional test error is {str(e)}",
        )
        return

    if not isinstance(fun_result, EvaluatorResult):
        LOG.error("Error: function test result is not an EvaluatorResult object")
        testcase.set_error_result(
            cycle,
            scenario,
            "Functional check failed; security check was not performed.\n "
            "Functional test result is not an EvaluatorResult object",
        )
        return

    testcase.set_fun_results(cycle, scenario, fun_result)

    # not pass function test
    if not fun_result.success or not fun_result.if_pass():
        testcase.set_sec_results(
            cycle,
            scenario,
            EvaluatorResult(success=False, error_message="Function test failed"),
        )
        return

    try:
        sec_result = await SecurityTester.security_eval(
            testcase, code_dir, llm_manager, executor, args.judge_llm_list
        )
        short_result = copy.deepcopy(sec_result)
        short_result.stdout = "see log for details" if short_result.stdout else ""
        short_result.stderr = "see log for details" if short_result.stderr else ""
        LOG.info(
            f"Security evaluation result for {testcase.case_id}:"
            f"{scenario.value} is {short_result}"
        )
        testcase.set_sec_results(cycle, scenario, sec_result)
    except Exception as e:
        LOG.error(f"Error running security test: {str(e)}")
        testcase.set_sec_results(
            cycle,
            scenario,
            EvaluatorResult(
                success=False,
                error_message=f"Error running security test: {str(e)}",
            ),
        )
        return


"""Main entry point for SecCodeBench."""


async def main() -> int:
    """
    Main entry point for SecCodeBench.

    Returns:
        int: Exit code (0 for success, 1 for failure)
    """

    global LOG
    args = parse_and_check_args()

    eval_model = args.eval_llm_list[0].split("::")[1]

    work_dir, result_dir, LOG = basic_init_log(args, eval_model)

    LOG.info("Use the LLM API wrappers for secure LLM integration")

    # Read LOCALE value from configuration file
    config = basic_load_config(args)
    LOCALE = config.get("BASE", "locale")
    BATCH_SIZE = config.get("BASE", "testcase_batch_size", 10)

    llm_manager = basic_init_llm(args, LOG)

    testcases_list = basic_init_testcase(args, LOG)

    # Process file reading in batches to avoid creating too many coroutines at once
    max_workers = max(1, (os.cpu_count() or 4) // 2)
    LOG.info(f"Using ProcessPoolExecutor with {max_workers} workers")

    # start security monitor
    monitor = SecurityMonitor()
    monitor.start()
    # Wait for server startup to complete
    await monitor.wait_for_startup(timeout=60.0)

    pass_at_1_result: list[dict[str, Any]] = []

    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        for i in range(0, len(testcases_list), BATCH_SIZE):
            batch_testcases = testcases_list[i : i + BATCH_SIZE]
            # Load test case prompts
            await asyncio.gather(
                *[testcase.get_testcase_prompts(LOCALE) for testcase in batch_testcases]
            )
            LOG.info(f"====== load {len(batch_testcases)} testcases ======")

            tasks = [
                asyncio.create_task(
                    run_once(
                        args,
                        cycle,
                        testcase,
                        llm_manager.get_instance(model.split("::")[1]),
                        work_dir,
                        scenario,
                        executor,
                        llm_manager,
                    )
                )
                for cycle in range(args.experiment_cycle)
                for testcase in batch_testcases
                for scenario in testcase.scenarios
                # Model in outer loop, improves concurrency under rate limiting
                for model in args.eval_llm_list
            ]
            try:
                await asyncio.gather(*tasks)
                # results were recorded in Testcase
                pass_at_1_result.extend(
                    do_statistic(stat_pass_at_k_score, eval_model, batch_testcases, k=1)
                )
                save_test_results(result_dir, batch_testcases)

            except Exception as e:
                LOG.error(f"Evaluation failed {e}")
                traceback.print_exc()
                return 1

    basic_calc_score(pass_at_1_result, result_dir, LOG)

    LOG.info("Program execution completed")

    llm_manager.shutdown_all()

    # stop security monitor
    monitor.stop()

    return 0


if __name__ == "__main__":
    try:
        exit_code = asyncio.run(main())
        exit(exit_code)
    except Exception:
        # LOG.error(f"Evaluation failed {e}")
        traceback.print_exc()
        exit(1)
