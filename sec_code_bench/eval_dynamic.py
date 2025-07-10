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
from typing import List

from .llms.llm import LLM, LLMConfig
from .llms.openai import OPENAI
from .testers.base_tester import TestResult
from .testers.maven_tester import MavenTester

logging.basicConfig(
    level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
LOG: logging.Logger = logging.getLogger(__name__)

LOCALE: str = "zh-CN"
SPECIFICATION_FORMAT: str = "<APISTYLE>::<MODEL>::<API KEY>::<URL>"
EXAMPLE_SPECIFICATION: str = (
    "OpenAI::gpt-3.5-turbo::<YOUR API KEY>::https://dashscope.aliyuncs.com/compatible-mode/v1"
)

TESTER_MAPPING = {
    "MavenTester": MavenTester,
}


def get_secure_score(case_results: List[TestResult]):
    pass_num = 0

    for result in case_results:
        if result.tests != 0 and result.failures == 0 and result.errors == 0:
            pass_num += 1

    return pass_num / len(case_results)


def handle_case(llm: LLM, case) -> TestResult:
    current_dir = os.path.dirname(os.path.abspath(__file__))

    prompts_file_path = (
        current_dir
        + "/../datasets/runnable/benchmark/"
        + case.get("language")
        + "/prompts/"
        + case.get("template")
        + "."
        + LOCALE
    )
    with open(prompts_file_path, "r", encoding="utf-8") as file:
        prompt = file.read()
    response = llm.query(prompt)
    LOG.info({"prompt": prompt, "response": response})

    code_template_dir = (
        current_dir
        + "/../datasets/runnable/templates/"
        + case.get("language")
        + "/"
        + case.get("template")
    )
    code_dir = "/tmp/" + str(uuid.uuid4())
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
            with open(
                code_dir + "/" + params.get(file_name), "w", encoding="utf-8"
            ) as f:
                f.write(trimmed_text)
        else:
            return TestResult(0, 0, 0, 0, "", "")

    tester_class = TESTER_MAPPING.get(case.get("tester"))
    tester = tester_class(code_dir)
    # run test case
    return tester.test()


def main():
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

    args = parser.parse_args()
    parts = args.llm_under_test.split("::")
    if parts[0] == "OPENAI":
        llmConfig = LLMConfig(parts[1], parts[3], parts[2])
        llm = OPENAI(llmConfig)
    else:
        LOG.fatal("Unknown API Format")

    # do test
    scores = []
    with open(args.benchmark, "r", encoding="utf-8") as f:
        data = json.load(f)
        for case in data:
            case_results = []
            for i in range(5):
                result = handle_case(llm, case)
                case_results.append(result)
                # result.stderr = ""
                # result.stdout = ""
                LOG.info(result)

            LOG.info(case.get("template"))
            score = get_secure_score(case_results)
            scores.append(score)
            LOG.info(score)

    LOG.info(f"The model must pass both of functional and security testcase currently.")
    LOG.info(f"Score: %f", sum(scores) / len(scores))

if __name__ == "__main__":
    main()
