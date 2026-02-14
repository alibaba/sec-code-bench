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
import json
import traceback
import logging
from pathlib import Path
from typing import Any

from sec_code_bench import (
    basic_calc_score,
    basic_checker,
    basic_init_llm,
    basic_init_log,
    basic_parser,
    init_testcases_auto_detect,
    parse_language_configs,
)
from sec_code_bench.config.system_config import SystemConfig
from sec_code_bench.evaluator.base import (
    EvaluatorResult,
)
from sec_code_bench.llm.llm_base import LLMBase
from sec_code_bench.llm.llm_manager import LLMManager
from sec_code_bench.statistic.pass_at_k_statistic import (
    stat_pass_at_k_score,
)
from sec_code_bench.statistic.statistic_manager import do_statistic
from sec_code_bench.tester.remote_verifier import RemoteVerifyResult, verify_code
from sec_code_bench.utils.testcase_storage import (
    save_test_results,
)

from sec_code_bench.utils.testcase import Testcase, TestScenario

# Module-level logger - initialized immediately, always available
LOG = logging.getLogger(__name__)


def normalize_llm_response(response: Any) -> str | None:
    """
    Normalize LLM response to a string.

    Some LLMs return a list instead of a string. This function handles
    both cases and converts list responses to a single string.

    Args:
        response: The raw response from the LLM, can be str or list.

    Returns:
        str: The normalized string response, or None if conversion failed.
    """
    if isinstance(response, str):
        return response

    if isinstance(response, list):
        combined_text = ""
        for item in response:
            if isinstance(item, dict) and "text" in item:
                combined_text += item["text"]
            elif isinstance(item, str):
                combined_text += item
        if combined_text:
            return combined_text

    return None


def parse_and_check_args() -> argparse.Namespace:
    """
    Parse command line arguments.

    Returns:
        argparse.Namespace: Parsed arguments object.
    """
    parser = basic_parser()
    args = parser.parse_args()

    basic_checker(args, parser)

    return args


async def attempt_fix_remote_code(
    testcase: Testcase,
    original_response: str,
    verify_result: RemoteVerifyResult,
    llm: LLMBase,
    parameters: dict[str, Any] | None = None,
) -> str:
    """
    Attempt to fix remote code based on function test failure.

    Args:
        testcase: Test case object.
        original_response: Original LLM response containing the code.
        verify_result: Remote verification result containing failure information.
        llm: LLM instance to use for fixing code.
        parameters: Optional parameters dict to pass to the LLM API call.

    Returns:
        str: Fixed code response from LLM.

    Raises:
        Exception: If LLM query fails or response cannot be processed.
    """
    # Build error message from verification result
    func_result = verify_result.functional_result
    error_parts = []

    error_parts.append("Function test failed with the following details:")
    error_parts.append(f"Total tests: {func_result.get('total_tests', 0)}")
    error_parts.append(f"Failures: {func_result.get('total_failures', 0)}")
    error_parts.append(f"Errors: {func_result.get('total_errors', 0)}")

    if verify_result.stderr:
        error_parts.append(f"\nError output:\n{verify_result.stderr}")

    if verify_result.stdout:
        error_parts.append(f"\nTest output:\n{verify_result.stdout}")

    error_message = "\n".join(error_parts)

    # Create fix prompt (similar to PROMPT_FixSyntaxError from universal_evaluator.py)
    fix_prompt = f"""You are a professional programmer. Your task is to fix the following code based on the function test errors.

Error information:
{error_message}

Original code:
----------------------
{original_response}
----------------------

Please analyze the errors and provide the fixed code. Only return the fixed code in the same format as the original response.
Return the complete fixed code wrapped in the same XML tags as the original response.

Important: Return only the fixed code with XML tags, no additional explanations."""

    LOG.info(f"Attempting to fix remote code for {testcase.case_id}")
    LOG.debug(f"Fix prompt:\n{fix_prompt}")

    # Query LLM for fix
    try:
        fixed_response = await llm.aquery(
            sys_prompt="You are a professional programmer.",
            user_prompt=fix_prompt,
            parameters=parameters,
        )
    except Exception as e:
        LOG.error(f"Error querying LLM for code fix: {e}")
        raise

    # Normalize response if needed
    if not isinstance(fixed_response, str):
        fixed_response = normalize_llm_response(fixed_response)
        if fixed_response is None:
            raise ValueError("Failed to normalize LLM fix response")

    return fixed_response


async def get_llm_response(
    testcase: Testcase,
    scenario: TestScenario,
    llm: LLMBase,
    parameters: dict[str, Any],
    cycle: int,
) -> str | None:
    """
    Get and normalize LLM response for a testcase.

    Args:
        testcase: Test case object.
        scenario: The test scenario to run.
        llm: LLM instance to use.
        parameters: Parameters dict to pass to the LLM API call.
        cycle: Current experiment cycle.

    Returns:
        str | None: Normalized response string, or None if failed.
    """
    try:
        prompt_content = testcase.get_scenario_prompt(scenario)
        response = await llm.aquery(
            sys_prompt="You are a professional programmer.",
            user_prompt=prompt_content,
            parameters=parameters,
        )
    except Exception as e:
        LOG.error(f"LLM query failed for {testcase.case_id}: {e}", exc_info=True)
        testcase.set_error_result(
            cycle, scenario, f"LLM query failed for {testcase.case_id}: {e}"
        )
        return None

    # Normalize response (handle list format from some LLMs, e.g. Gemini-3-Pro)
    if not isinstance(response, str):
        LOG.info(
            f"LLM returned non-string response for {testcase.case_id}: {type(response)}, "
            "attempting to normalize..."
        )
        response = normalize_llm_response(response)
        if response is None:
            LOG.error(f"Failed to normalize LLM response for {testcase.case_id}")
            testcase.set_error_result(
                cycle,
                scenario,
                f"Failed to normalize LLM response for {testcase.case_id}",
            )
            return None
        LOG.info(f"Successfully normalized list response for {testcase.case_id}")

    return response


async def verify_code_with_retry(
    testcase: Testcase,
    scenario: TestScenario,
    cycle: int,
    response: str,
    verify_url: str,
    prompt_path: str | None,
    judge_llm_list_for_api: list[str] | None,
    llm: LLMBase,
    parameters: dict[str, Any],
    max_attempts: int = 3,
) -> RemoteVerifyResult | None:
    """
    Verify code with retry logic for function test failures.

    Args:
        testcase: Test case object.
        scenario: The test scenario to run.
        cycle: Current experiment cycle.
        response: Initial LLM response to verify.
        verify_url: URL for remote verification API.
        prompt_path: Path to prompt file.
        judge_llm_list_for_api: List of judge LLMs for verification.
        llm: LLM instance for code fixing.
        parameters: Parameters dict to pass to the LLM API call.
        max_attempts: Maximum number of verification attempts.

    Returns:
        RemoteVerifyResult | None: Verification result, or None if failed.
    """
    verify_result = None
    current_response = response

    for attempt in range(1, max_attempts + 1):
        LOG.info(
            f"Remote eval attempt {attempt}/{max_attempts} "
            f"for {testcase.case_id}:{scenario.value}"
        )

        # Save the response for this attempt
        testcase.set_generated_code(cycle, scenario, current_response)

        # Send response to remote verification API
        try:
            verify_result = await verify_code(
                url=verify_url,
                code=current_response,
                prompt_path=prompt_path,
                judge_llm_list=judge_llm_list_for_api,
            )
        except Exception as e:
            LOG.error(
                f"Remote verification failed for {testcase.case_id} (attempt {attempt}): {e}",
                exc_info=True,
            )
            if attempt == max_attempts:
                testcase.set_error_result(
                    cycle,
                    scenario,
                    f"Remote verification failed after {max_attempts} attempts: {e}",
                )
                return None
            LOG.warning(f"Will retry verification (attempt {attempt + 1}/{max_attempts})")
            continue

        # Check if there was an error during verification
        if verify_result.error_message:
            LOG.error(
                f"Remote verification error for {testcase.case_id} (attempt {attempt}): "
                f"{verify_result.error_message}"
            )
            if attempt == max_attempts:
                testcase.set_error_result(
                    cycle,
                    scenario,
                    f"Remote verification error after {max_attempts} attempts: {verify_result.error_message}",
                )
                return None
            LOG.warning(f"Will retry verification (attempt {attempt + 1}/{max_attempts})")
            continue

        # Check if function test passed
        if verify_result.functional_pass:
            LOG.info(
                f"Remote functional test passed for {testcase.case_id}:{scenario.value} on attempt {attempt}"
            )
            break

        # Function test failed
        LOG.warning(
            f"Remote functional test failed for {testcase.case_id}:{scenario.value} on attempt {attempt}/{max_attempts}"
        )

        # If this is not the last attempt, try to fix the code
        if attempt < max_attempts:
            try:
                LOG.info(
                    f"Attempting to fix code for {testcase.case_id} (attempt {attempt + 1}/{max_attempts})"
                )
                fixed_response = await attempt_fix_remote_code(
                    testcase, current_response, verify_result, llm, parameters
                )
                # Normalize the fixed response if needed
                if not isinstance(fixed_response, str):
                    fixed_response = normalize_llm_response(fixed_response)
                    if fixed_response is None:
                        LOG.error(
                            f"Failed to normalize fixed response for {testcase.case_id}"
                        )
                        testcase.set_error_result(
                            cycle, scenario, "Failed to normalize fixed code response"
                        )
                        return None
                current_response = fixed_response
                LOG.info(
                    f"Code fix completed for {testcase.case_id}, will retry verification"
                )
            except Exception as e:
                LOG.error(
                    f"Failed to fix code for {testcase.case_id}: {e}", exc_info=True
                )
                LOG.warning(f"Using last verification result due to fix failure")
                break
        else:
            LOG.info(f"Max attempts reached for {testcase.case_id}:{scenario.value}")

    return verify_result


def save_functional_test_results(
    testcase: Testcase,
    cycle: int,
    scenario: TestScenario,
    verify_result: RemoteVerifyResult,
) -> None:
    """
    Save functional test results to testcase.

    Args:
        testcase: Test case object.
        cycle: Current experiment cycle.
        scenario: The test scenario.
        verify_result: Remote verification result.
    """
    func_result = verify_result.functional_result
    # Use function-specific stdout/stderr if available, otherwise fall back to general stdout/stderr
    # Note: Check for None explicitly, not truthiness, to allow empty strings
    func_stdout = (
        verify_result.function_stdout
        if verify_result.function_stdout is not None
        else verify_result.stdout
    )
    func_stderr = (
        verify_result.function_stderr
        if verify_result.function_stderr is not None
        else verify_result.stderr
    )
    fun_eval_result = EvaluatorResult(
        tests=func_result.get("total_tests", 0),
        failures=func_result.get("total_failures", 0),
        errors=func_result.get("total_errors", 0),
        skipped=func_result.get("total_skipped", 0),
        stdout=func_stdout,
        stderr=func_stderr,
        success=verify_result.functional_pass,
        error_message="" if verify_result.functional_pass else "Functional tests failed",
    )
    testcase.set_fun_results(cycle, scenario, fun_eval_result)

    LOG.info(
        f"Remote functional eval for {testcase.case_id}:{scenario.value} - "
        f"pass={verify_result.functional_pass}, "
        f"tests={func_result.get('total_tests', 0)}, "
        f"failures={func_result.get('total_failures', 0)}"
    )


def save_security_test_results(
    testcase: Testcase,
    cycle: int,
    scenario: TestScenario,
    verify_result: RemoteVerifyResult,
) -> None:
    """
    Save security test results to testcase.

    Args:
        testcase: Test case object.
        cycle: Current experiment cycle.
        scenario: The test scenario.
        verify_result: Remote verification result.
    """
    sec_result = verify_result.security_result
    # Use security-specific stdout/stderr if available, otherwise fall back to general stdout/stderr
    # Note: Check for None explicitly, not truthiness, to allow empty strings
    sec_stdout = (
        verify_result.security_stdout
        if verify_result.security_stdout is not None
        else verify_result.stdout
    )
    sec_stderr = (
        verify_result.security_stderr
        if verify_result.security_stderr is not None
        else verify_result.stderr
    )
    sec_eval_result = EvaluatorResult(
        tests=sec_result.get("total_tests", 0),
        failures=sec_result.get("total_failures", 0),
        errors=sec_result.get("total_errors", 0),
        skipped=sec_result.get("total_skipped", 0),
        stdout=sec_stdout,
        stderr=sec_stderr,
        success=verify_result.security_pass,
        error_message="" if verify_result.security_pass else "Security tests failed",
    )
    testcase.set_sec_results(cycle, scenario, sec_eval_result)

    LOG.info(
        f"Remote security eval for {testcase.case_id}:{scenario.value} - "
        f"pass={verify_result.security_pass}, "
        f"tests={sec_result.get('total_tests', 0)}, "
        f"failures={sec_result.get('total_failures', 0)}"
    )


async def run_once_remote(
    _args: argparse.Namespace,
    cycle: int,
    testcase: Testcase,
    llm: LLMBase,
    scenario: TestScenario,
    parameters: dict[str, Any],
) -> None:
    """
    Handle one remote test case evaluation for a specific scenario with retry on function test failures.

    For remote testcases, we:
    1. Send the query prompt to the LLM
    2. Send the LLM response to the remote verification API
    3. If function tests fail, attempt to fix the code and retry (up to 3 attempts total)
    4. Map the API response to EvaluatorResult

    Args:
        _args: Command line arguments.
        cycle: Current experiment cycle.
        testcase: Test case to handle (must be a remote testcase).
        llm: LLM instance to use.
        scenario: The test scenario to run.
        parameters: Optional parameters dict to pass to the LLM API call.
    """
    # Get initial LLM response
    response = await get_llm_response(testcase, scenario, llm, parameters, cycle)
    if response is None:
        return

    # Get verification URL
    verify_url = testcase.get_verify_url(scenario)
    if not verify_url:
        LOG.error(
            f"No verification URL for remote testcase {testcase.case_id} scenario {scenario}"
        )
        testcase.set_error_result(
            cycle,
            scenario,
            f"No verification URL for remote testcase {testcase.case_id} scenario {scenario}",
        )
        return

    prompt_path = testcase.get_remote_prompt_path(scenario)

    # Get judge_llm_list from args if available
    judge_llm_list_for_api = None
    if hasattr(_args, "judge_llm_list") and _args.judge_llm_list:
        judge_llm_list_for_api = _args.judge_llm_list
        LOG.info(
            f"Passing {len(judge_llm_list_for_api)} judge LLMs to remote verification API"
        )

    # Verify code with retry logic
    verify_result = await verify_code_with_retry(
        testcase=testcase,
        scenario=scenario,
        cycle=cycle,
        response=response,
        verify_url=verify_url,
        prompt_path=prompt_path,
        judge_llm_list_for_api=judge_llm_list_for_api,
        llm=llm,
        parameters=parameters,
        max_attempts=3,
    )

    # Check if we have a valid result
    if verify_result is None:
        LOG.error(f"No valid verification result for {testcase.case_id} after retries")
        testcase.set_error_result(
            cycle, scenario, f"No valid verification result after retries"
        )
        return

    # Save functional test results to Testcase (no disk write)
    save_functional_test_results(testcase, cycle, scenario, verify_result)

    # If functional test failed, set security test to failed as well
    if not verify_result.functional_pass:
        testcase.set_sec_results(
            cycle,
            scenario,
            EvaluatorResult(success=False, error_message="Function test failed"),
        )
        LOG.info(
            f"Remote security eval for {testcase.case_id}:{scenario.value} - "
            f"skipped due to functional test failure"
        )
        return

    # Save security test results
    save_security_test_results(testcase, cycle, scenario, verify_result)


"""Main entry point for SecCodeBench."""


async def main_remote(
    args: argparse.Namespace,
    testcases_list: list[Testcase],
    llm_manager: LLMManager,
    result_dir: Path,
    parameters: dict[str, Any],
    eval_model: str,
) -> tuple[int, list[dict[str, Any]]]:
    """
    Run evaluation using remote API verification.

    Args:
        args: Command line arguments.
        testcases_list: List of remote testcases.
        llm_manager: LLM manager instance.
        result_dir: Directory to save results.
        parameters: Optional LLM parameters.
        eval_model: Name of the evaluation model.
    Returns:
        Tuple of (exit code, list of pass@1 results).
    """
    LOG.info("Running in REMOTE verification mode")
    pass_at_1_result: list[dict[str, Any]] = []

    batch_size = args.batch_size if args.batch_size else 15
    for i in range(0, len(testcases_list), batch_size):
        batch_testcases = testcases_list[i : i + batch_size]

        # (testcases with prompt field but empty prompts dict)
        for testcase in batch_testcases:
            if testcase.prompt and not testcase.prompts:
                # This is a converted remote testcase - load prompts from files
                await testcase.get_testcase_prompts(testcase.locale)

        LOG.info(f"=== Processing {len(batch_testcases)} remote testcases ===")

        try:
            tasks = [
                asyncio.create_task(
                    run_once_remote(
                        _args=args,
                        cycle=cycle,
                        testcase=testcase,
                        llm=llm_manager.get_instance(model.split("::")[1]),
                        scenario=scenario,
                        parameters=parameters,
                    )
                )
                for cycle in range(args.experiment_cycle)
                for testcase in batch_testcases
                for scenario in testcase.scenarios
                for model in [args.eval_llm]
            ]

            await asyncio.gather(*tasks)
            pass_at_1_result.extend(
                do_statistic(
                    plugin=stat_pass_at_k_score,
                    model=eval_model,
                    testcases=batch_testcases,
                    k=1,
                )
            )
            # save test results to disk
            save_test_results(result_dir, batch_testcases)
        except Exception as e:
            LOG.error(f"Remote evaluation failed: {e}")
            traceback.print_exc()
            return 1, pass_at_1_result

    return 0, pass_at_1_result


async def main() -> int:
    """
    Main entry point for SecCodeBench.

    Returns:
        int: Exit code (0 for success, 1 for failure)
    """
    args = parse_and_check_args()

    eval_model = args.eval_llm.split("::")[1]

    # Initialize logging system - this configures the module-level LOG
    result_dir, _ = basic_init_log(args, eval_model)

    LOG.info("Use the LLM API wrappers for secure LLM integration")

    # Load system configuration
    system_config = SystemConfig.load()

    # Parse parameters from command line if provided
    parameters: dict[str, Any] = {}
    if args.parameters:
        try:
            parameters = json.loads(args.parameters)
            LOG.info(f"Using parameters: {parameters}")
        except json.JSONDecodeError as e:
            LOG.error(f"Failed to parse parameters JSON: {e}")
            return 1

    llm_manager = basic_init_llm(args, LOG)

    # Parse language configurations first to get benchmark paths
    try:
        lang_configs = parse_language_configs(args)
    except ValueError as e:
        LOG.error(str(e))
        return 1

    if not lang_configs:
        LOG.error("No language configurations found")
        return 1

    # Load testcases for each language config, detecting format individually
    remote_testcases: list[Testcase] = []

    for lang_config in lang_configs:
        LOG.info(
            f"Loading testcases for {lang_config.language} "
            f"from {lang_config.benchmark_path}"
        )
        try:
            testcases = init_testcases_auto_detect(
                lang_config.benchmark_path, lang_config.language, LOG
            )
        except Exception as e:
            LOG.error(f"Failed to load testcases for {lang_config.language}: {e}")
            return 1

        remote_testcases.extend(testcases)
        LOG.info(f"  -> {len(testcases)} remote testcases for {lang_config.language}")

    total_testcases = len(remote_testcases)
    if total_testcases == 0:
        LOG.error("No testcases loaded")
        return 1

    LOG.info(f"Loaded {total_testcases} testcases total ")

    exit_code = 0
    all_pass_at_1_results: list[dict[str, Any]] = []

    try:
        if remote_testcases:
            LOG.info(f"Running {len(remote_testcases)} REMOTE testcases...")
            remote_exit_code, remote_results = await main_remote(
                args, remote_testcases, llm_manager, result_dir, parameters, eval_model
            )
            all_pass_at_1_results.extend(remote_results)
            if remote_exit_code != 0:
                exit_code = remote_exit_code

        # Calculate final score from all results
        if all_pass_at_1_results:
            basic_calc_score(all_pass_at_1_results, result_dir, LOG, system_config)

    finally:
        llm_manager.shutdown_all()

    LOG.info("Program execution completed")

    return exit_code


if __name__ == "__main__":
    try:
        exit_code = asyncio.run(main())
        exit(exit_code)
    except Exception:
        traceback.print_exc()
        exit(1)
