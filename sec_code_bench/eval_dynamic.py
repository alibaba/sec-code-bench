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
import os
import re
import shutil
import uuid
import sys
import asyncio
import aiofiles
from typing import List, Tuple
from pathlib import Path
from datetime import datetime
from pathlib import Path
from concurrent.futures import ProcessPoolExecutor

from llms.llm import LLM, LLMConfig
from llms.openai import OPENAI
from testers.base_tester import TestResult
from testers.maven_tester import MavenTester
from utils.fdisk_utils import get_contents_async
from utils.rate_limiter import AsyncRateLimiter
from testers.security_monitor import SecurityMonitor

LOG: logging.Logger = logging.getLogger(__name__)

LOCALE: str = "zh-CN"
SPECIFICATION_FORMAT: str = "<APISTYLE>::<MODEL>::<API KEY>::<URL>"
EXAMPLE_SPECIFICATION: str = (
    "OpenAI::gpt-3.5-turbo::<YOUR API KEY>::https://dashscope.aliyuncs.com/compatible-mode/v1"
)

TESTER_MAPPING = {
    "MavenTester": MavenTester,
}

# EXPERIMENT_CYCLE moved to command line argument


def get_secure_score(case_results: List[TestResult]):
    pass_num = 0

    for result in case_results:
        if result.tests != 0 and result.failures == 0 and result.errors == 0:
            pass_num += 1

    return pass_num / len(case_results)

def run_test_in_process(tester_class, code_dir):
    print(f"Running test in process, test {code_dir}")
    tester = tester_class(code_dir)
    return tester.test()

async def handle_case(llm: LLM, case, prompt, executor) -> Tuple[TestResult, TestResult]:
    current_dir = os.path.dirname(os.path.abspath(__file__))

    try:
        # LOG.info(f"Processing { case.get("params").keys()} ...")
        response = await llm.aquery(prompt)
        if not isinstance(response, str):
            LOG.error(f"LLM returned non-string response for {case.get('prompt')}: {type(response)}")
            raise Exception("LLM returned non-string response")
    except Exception as e:
        LOG.error(f"LLM query failed for {case.get('prompt')}: {str(e)}")
        await asyncio.sleep(10)
        try:
            response = await llm.aquery(prompt)
        except Exception as e2:
            LOG.error(f"LLM query failed again for {case.get('prompt')}: {str(e2)}")
            return TestResult(0, 0, 0, 0, "", ""), TestResult(0, 0, 0, 0, "", "")

    # LOG.info({"prompt": prompt, "response": response})

    code_template_dir = (
        current_dir
        + "/../datasets/runnable/templates/"
        + case.get("language")
        + "/"
        + case.get("template")
    )
    tmp_dir = "/tmp/sec-code-bench-dynamic/"
    os.makedirs(tmp_dir, exist_ok=True)
    code_dir = tmp_dir + str(uuid.uuid4())
    shutil.copytree(code_template_dir, code_dir)

    # Substitute the code with model-generated code
    params = case.get("params")
    for file_name in params.keys():
        pattern = "<" + file_name + ">" + """(.*?)""" + "</" + file_name + ">"
        match = re.search(pattern, response, re.DOTALL)

        if match:
            content = match.group(1).strip().rstrip()
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
            async with aiofiles.open(
                code_dir + "/" + params.get(file_name), "w", encoding="utf-8"
            ) as f:
                await f.write(trimmed_text)
        else:
            LOG.error(f"No match found for {file_name}")
            return TestResult(0, 0, 0, 0, "", ""), TestResult(0, 0, 0, 0, "", "")

    tester_class = TESTER_MAPPING.get(case.get("tester"))

    loop = asyncio.get_running_loop()
    functional_result, security_result = await loop.run_in_executor(
        executor, 
        run_test_in_process, 
        tester_class, 
        code_dir
    )
    
    return functional_result, security_result

def setup_logging(logfile):
    logger = logging.getLogger()
    if logger.hasHandlers():
        return
    formatter = logging.Formatter("%(asctime)s [%(levelname)s] %(name)s: %(message)s")
    sh = logging.StreamHandler(sys.stdout)
    sh.setFormatter(formatter)
    logger.addHandler(sh)
    if logfile:
        fh = logging.FileHandler(logfile)
        fh.setFormatter(formatter)
        logger.addHandler(fh)
    logger.setLevel(logging.INFO)
    # logger.setLevel(logging.DEBUG)


async def main():
    parser = argparse.ArgumentParser(description="Run the SecCode Benchmarks.")

    parser.add_argument(
        "--benchtype",
        help="This is the argument to specify which benchmark to run. Currently supported benchmarks are: autocomplete, instruct.",
    )

    parser.add_argument(
        "--benchlanguage",
        required=True,
    )

    parser.add_argument(
        "--llm-under-test",
        required=True,
        help=f"LLM to benchmark provided as {SPECIFICATION_FORMAT}, e.g., {EXAMPLE_SPECIFICATION}",
    )
    parser.add_argument(
        "--benchmark",
        help="test file",
    )
    
    parser.add_argument(
        "--experiment-cycle",
        type=int,
        default=5,
        help="Number of experiment cycles for each test case (default: 5)",
    )

    args = parser.parse_args()
    parts = args.llm_under_test.split("::")
    if parts[0] == "OPENAI":
        llmConfig = LLMConfig(parts[1], parts[3], parts[2])
        limiter = AsyncRateLimiter(
            max_cnts=60,
            window_seconds=60,
            burst_size=1
        )
        llm = OPENAI(llmConfig, limiter)
    else:
        LOG.fatal(f"Unknown API Format: {parts[0]}")
        return 1

    # start security monitor
    monitor = SecurityMonitor()
    monitor.start()

    # setup logging
    dir_path = Path("./logs")
    dir_path.mkdir(parents=True, exist_ok=True)
    log_filename = f"logs/{parts[1]}-{datetime.now().strftime('%Y%m%d_%H%M%S')}.log"
    setup_logging(log_filename)

    # do test
    try:
        with open(args.benchmark, "r", encoding="utf-8") as f:
            data = json.load(f)
        LOG.info(f"Loaded {len(data)} test cases")
    except Exception as e:
        LOG.fatal(f"Failed to load benchmark file: {str(e)}")
        return 1

    current_dir = os.path.dirname(os.path.abspath(__file__))
    testcases_paths = [f"{current_dir}/../datasets/runnable/benchmark/{case.get("language")}"
                       f"/prompts/{case.get("prompt")}.{LOCALE}" for case in data]
    
    testcases_prompts = await get_contents_async(testcases_paths)
    
    max_workers = max(1, (os.cpu_count() or 4) // 2)
    LOG.info(f"Using ProcessPoolExecutor with {max_workers} workers")
        
    with ProcessPoolExecutor(max_workers=max_workers) as executor:
        try:
            tasks = [handle_case(llm, case, prompt, executor) 
                for case, prompt in zip(data, testcases_prompts) 
                for _ in range(args.experiment_cycle)]    
            results = await asyncio.gather(*tasks)
            
        except Exception as e:
            LOG.exception("Evaluation failed")
            return 1
    
    scores = []
    for i, case in enumerate(data):
        case_results = []
        LOG.info(case.get("prompt"))
        LOG.info(f"Progress: {len(scores)+1}/{len(data)}")
        
        for j in range(args.experiment_cycle):
            idx = i * args.experiment_cycle + j
            if idx < len(results):
                func_result, sec_result = results[idx]
                func_result.stderr = ""
                func_result.stdout = ""
                sec_result.stderr = ""
                sec_result.stdout = ""
                LOG.info(f"FunctionalResult-{func_result}")
                LOG.info(f"SecurityResult-{sec_result}")
                case_results.append(TestResult(func_result.tests+sec_result.tests, func_result.failures+sec_result.failures, func_result.errors+sec_result.errors, func_result.skipped+sec_result.skipped, "", ""))

        score = get_secure_score(case_results)
        scores.append(score)
        LOG.info(f"append score {score}")

    LOG.info(f"The model must pass both of functional and security testcase currently.")
    LOG.info(f"Score: %f", sum(scores) / len(scores))
    LOG.info(parts[1]+"::"+parts[3])

    # close llm connection
    await llm.aclose()
    # stop security monitor
    monitor.stop()
    shutil.rmtree("/tmp/sec-code-bench-dynamic/")

if __name__ == "__main__":
    asyncio.run(main())
