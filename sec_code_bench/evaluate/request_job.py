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

from dataclasses import dataclass
from datetime import datetime
from enum import Enum

from parser.testcase.vulnerability_schema import VulnerabilitySchema


class Stage(Enum):
    GENERATION = "generation"
    EVALUATION = "evaluation"


class JobType(Enum):
    # Generate
    INSTRUCTION_GEN = "instruction_gen"
    COMPLETION_GEN = "completion_gen"

    # Eval
    LLM_EVAL = "llm_eval"
    SAST_EVAL = "sast_eval"


@dataclass
class RequestJob:
    id: str
    timestamp: datetime
    model: str
    job_type: JobType
    vulnerability_schema: VulnerabilitySchema


@dataclass
class GenerationJob(RequestJob):
    generation_prompt: str
    content: str
    path: str = ""


@dataclass
class StaticEvalJob(RequestJob):
    eval_prompt: str
    llms_as_judge: list[str]
    name: str = ""
