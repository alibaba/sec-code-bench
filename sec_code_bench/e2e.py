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
import concurrent
import concurrent.futures
import logging
import os
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from typing import Any

from tqdm import tqdm

from sec_code_bench import (
    basic_calc_score,
    basic_checker,
    basic_init_llm,
    basic_init_log,
    basic_init_testcase,
    basic_load_config,
    basic_parser,
)
from sec_code_bench.editor import Editor, EditorFactory, IDEType
from sec_code_bench.llm.llm_manager import LLMManager
from sec_code_bench.runner.runner import Runner
from sec_code_bench.statistic.pass_at_k_statistic import stat_pass_at_k_score
from sec_code_bench.statistic.statistic_manager import do_statistic
from sec_code_bench.utils.fdisk_utils import save_test_results
from sec_code_bench.utils.logger_utils import Logger
from sec_code_bench.utils.testcase import Testcase, TestScenario

LOG: logging.Logger | None = None


def ignore_src_test(src: str, names: list[str]) -> set[str]:
    """
    Ignore src/test directory during copy to avoid knowing the answer in advance.

    Args:
        src: Source directory path.
        names: Names in the source directory.

    Returns:
        Set of directory names to ignore.
    """
    if os.path.basename(src) == "src" and "test" in names:
        return {"test"}
    return set()


def parse_and_check_args() -> argparse.Namespace:
    """
    Parse command line arguments.

    Returns:
        Parsed arguments object.
    """
    parser = basic_parser()

    parser.add_argument(
        "--editor",
        "-e",
        choices=IDEType,
        type=str,
        default="vscode",
        help="Specify the editor type to be used, default is vscode",
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
        help="Specify the number of worker threads for parallel execution (default: 1)",
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
            "Filter testcases: use range like '0-4' for indices"
            "or string for exact/partial key matching (exact match preferred). Empty means all testcases."
        )
    )

    args = parser.parse_args()

    basic_checker(args, parser)

    return args


def run_once(
    args: argparse.Namespace,
    cycle: int,
    editor: Editor,
    testcase: Testcase,
    scenario: TestScenario,
    work_dir: Path,
    llm_manager: LLMManager,
) -> None:
    """
    Run a single test case.

    Args:
        args: Command line arguments.
        cycle: Current experiment cycle.
        editor: Editor instance.
        testcase: Test case to run.
        scenario: Scenario to test.
        work_dir: Working directory.
        llm_manager: LLM manager instance.

    Returns:
        None
    """
    runner = Runner(
        testcase,
        scenario,
        editor,
        cycle,
        args.prepare,
    )
    runner.run(work_dir, llm_manager, args.judge_llm_list)


def main() -> int:
    """
    Main entry point for run the E2E test cases.

    Returns:
        Exit code (0 for success, non-zero for error).
    """
    global LOG
    args = parse_and_check_args()

    work_dir, result_dir, LOG = basic_init_log(args, args.editor)

    LOG.info("Use IDE auto wrappers for secure LLM integration")

    testcases_list = basic_init_testcase(args, LOG)

    llm_manager = basic_init_llm(args, LOG)

    # Read LOCALE value from configuration file
    config = basic_load_config(args)

    # init editor
    editor = EditorFactory.get_editor(args.editor)
    # Process file reading in batches to avoid creating too many coroutines at once
    max_workers = (
        args.threads if hasattr(args, "threads") else max(1, (os.cpu_count() or 4) // 2)
    )
    LOG.info(f"Using ThreadPoolExecutor with {max_workers} workers")

    pass_at_1_result: list[dict[str, Any]] = []

    if args.prompt:
        filtered_testcases = []

        # Check if prompt is a numeric range (e.g., "0-4", "1-10")
        if "-" in args.prompt and all(part.strip().isdigit() for part in args.prompt.split("-")):
            try:
                start, end = map(int, args.prompt.split("-"))
                if start < 0 or end < start:
                    LOG.error(f"Invalid range: {args.prompt}. Start must be >= 0 and end must be >= start.")
                    return 1

                # Get testcases by index range
                if end >= len(testcases_list):
                    LOG.warning(f"End index {end} exceeds available testcases ({len(testcases_list)}). Using all available testcases from index {start}.")
                    end = len(testcases_list) - 1

                filtered_testcases = testcases_list[start:end+1]
                LOG.info(f"Selected testcases by range {start}-{end}: {len(filtered_testcases)} testcases")

            except ValueError:
                LOG.error(f"Invalid range format: {args.prompt}. Use format like '0-4'.")
                return 1
        else:
            # String matching logic - exact match first, then partial match
            exact_matches = []
            partial_matches = []

            for testcase in testcases_list:
                if args.prompt.lower() == testcase.case_id.lower():
                    exact_matches.append(testcase)
                elif args.prompt.lower() in testcase.case_id.lower():
                    partial_matches.append(testcase)

            # Prefer exact matches, fall back to partial matches if no exact match
            if exact_matches:
                filtered_testcases = exact_matches
                LOG.info(f"Found exact match for '{args.prompt}': {len(filtered_testcases)} testcase(s)")
            elif partial_matches:
                filtered_testcases = partial_matches
                LOG.info(f"Found partial matches for '{args.prompt}': {len(filtered_testcases)} testcase(s)")
                # Log the matched testcase names for clarity
                matched_names = [tc.case_id for tc in partial_matches]
                LOG.info(f"Matched testcases: {', '.join(matched_names)}")
            else:
                LOG.warning(f"No testcases found matching pattern '{args.prompt}'")
                return 1

        testcases_list = filtered_testcases

    LOG.info(f"Using {len(testcases_list)} testcases")

    for case in testcases_list:
        case.get_testcase_prompts_sync(config.get("BASE", "locale"))

    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        future_to_task = {
            executor.submit(
                run_once,
                args,
                cycle,
                editor,
                testcase,
                scenario,
                work_dir,
                llm_manager,
            ): (cycle, editor, testcase, scenario, work_dir, llm_manager)
            for cycle in range(args.experiment_cycle)
            for testcase in testcases_list
            for scenario in testcase.scenarios
        }
        with tqdm(
            total=len(future_to_task),
            ncols=125,
            position=0,
            desc="E2E Testcase Running...",
            leave=True,
            dynamic_ncols=True,
            ascii=True,
            smoothing=0.1,
        ) as pbar:
            # Set tqdm instance for coordinated logging
            Logger.set_tqdm_instance(pbar)
            for future in concurrent.futures.as_completed(future_to_task):
                pbar.update(1)
                task_info = future_to_task[future]
                (cycle, editor, testcase, scenario, work_dir, llm_manager) = task_info

                pass_at_1_result.extend(
                    do_statistic(stat_pass_at_k_score, args.editor, [testcase], k=1)
                )

                save_test_results(result_dir, [testcase])

    basic_calc_score(pass_at_1_result, result_dir, LOG)

    llm_manager.shutdown_all()

    return 0


if __name__ == "__main__":
    sys.exit(main())
