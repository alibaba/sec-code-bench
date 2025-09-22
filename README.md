<h1>
  <img src="docs/images/logo.png" alt="SecCodeBench Logo" width="50" style="vertical-align: middle;"> 
  SecCodeBench
</h1>

![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)
![Language](https://img.shields.io/badge/language-Python3-orange.svg)
![Status](https://img.shields.io/badge/status-active-brightgreen.svg)

<div align="middle">

[**English**] · [**简体中文**](./README.zh-CN.md)

</div>

SecCodeBench is a benchmark suite for evaluating the security of AI-generated code, specifically designed for modern Agentic Coding Tool. It is jointly developed by Alibaba Group in collaboration with the the Institute for Network Sciences and Cyberspace at Tsinghua University, the School of CyberScience and Technology at Zhejiang University, Fudan University, and Peking University.

## 📖 Overview

With the proliferation of Large Language Model (LLM)-powered coding assistants, **the security of AI-generated code has become a critical concern**. To scientifically evaluate the security of AI-generated code, identify its intrinsic flaws, and foster improvements in model security capabilities, a comprehensive and reliable benchmark is essential.

However, existing security benchmarks in the community suffer from significant limitations across three core dimensions, making them inadequate for authentically assessing the secure coding capabilities of models or Agents:

However, existing security benchmarks in the community suffer from significant limitations across three core dimensions, making them inadequate for authentically assessing the secure coding capabilities of models or Agentic Coding Tools:

*   **Test Case Quality**: Many datasets are sourced from open-source repositories, relying heavily on automated generation and simplistic filtering with minimal deep human involvement. This leads to: **(a) Data Imbalance**, where a large volume of low-priority security issues predominates, failing to effectively measure model performance on critical vulnerabilities; **(b) Invalid Test Cases**, where some problems are flawed by design (e.g., generating secure code is impossible under the given constraints), causing a systematic underestimation of model capabilities rather than an objective evaluation; and **(c) Potential Data Contamination**, where the source code of the test cases may have been part of the models' pre-training corpus, thus compromising the fairness of the evaluation.

*   **Singular and Imprecise Evaluation Methods**: Most existing evaluation methods **rely on simple regular expressions or static analysis tools**. This makes them incapable of accurately identifying syntactically or semantically complex code variants and completely overlooks dynamic vulnerabilities that can only be verified through live execution. More importantly, **many methods neglect the importance of functionality,** leading to a disconnect between evaluation criteria and real-world usability, and may even favor non-functional "secure" code over correct solutions.

*   **Failure to Cover Agentic Coding Tools**: Real-world programming has evolved to rely on agentic coding tools—intelligent agents capable of autonomously using tools and retrieving knowledge. Existing evaluation paradigms, however, remain stuck at testing atomic API calls. This creates a disconnect between the evaluation paradigm and real-world application scenarios, limiting the practical value of their conclusions.

To address these challenges, we introduce `SecCodeBench`, a benchmark suite purpose-built for **modern Agentic Coding Tools**. It ensures evaluation depth and breadth through three core design principles:

*   **Dataset**: We ensure the authenticity and diversity of our test cases. Most of the cases are based on **anonymized, real-world historical vulnerabilities from within Alibaba** and are presented as complete, runnable projects rather than mere code snippets. Each test case is uniquely defined by four attributes: **(Functional Requirements, Programming Language, Third-Party Libraries, Function Interface)**. Currently, it includes 37 test cases covering 16 CWE types, adapted into four testing modes: Code Generation (native/security-aware) and Code Fix (native/security-aware). Each test case is crafted by a team of senior security experts and undergoes a rigorous three-person peer review. Furthermore, all cases have been subjected to multiple rounds of empirical testing and fine-tuning across more than ten models to ensure their fairness and challenge.

*   **Evaluation**: We have established a **multi-stage, high-precision evaluation process**. This process is governed by a **"Functionality-First" principle**, where generated code must first pass all functional tests to qualify for security assessment. The security evaluation employs a layered strategy: it **prioritizes dynamic execution-based validation using Proof-of-Concept (PoC) exploits** to ensure objective and reliable results. For complex scenarios not coverable by dynamic execution, we introduce an LLM-as-a-Judge infused with domain-specific security knowledge. The final score is a weighted sum of the pass@1 results, where the weights holistically consider factors such as the **test scenario** (with a 4:1 ratio for native vs. security-aware modes) and a combined metric of **vulnerability prevalence and severity** (assigned weights of 4, 2, and 1 for high, medium, and low tiers, respectively). This sophisticated scoring mechanism is designed to provide a more authentic reflection of the model's comprehensive security capabilities.

*   **Framework**: We provide a highly extensible testing framework. It not only supports standard multi-turn dialogue testing of model APIs but also enables **end-to-end automated evaluation of mainstream agentic coding tools (e.g., IDE plugins, CLI tools)**. Additionally, the framework generates **[comprehensive, visual reports and logs](https://alibaba.github.io/sec-code-bench)** to facilitate in-depth analysis and model diagnostics, thereby driving continuous improvement in the secure coding capabilities of large models.

## 🔬 Evaluation Workflow
![Workflow](./docs/images/workflow.png)

## 🚀 Getting Started

To ensure the reproducibility of our results, we strongly recommend using an official release of this project rather than cloning directly from the main branch.

### Download
Clone a specific version of the repository using the following commands:

```bash
# Clone the repository
git clone https://github.com/alibaba/sec-code-bench.git
cd sec-code-bench

# Check out the desired version tag
git checkout v2.0.0
```

### Environment Setup
- Python: 3.11 or higher
- Java: JDK 17
- Maven: 3.6+ or higher (for building and managing Java projects)

Install uv (if not already installed) for project management and dependency synchronization:
```bash
# Install uv
curl -LsSf https://astral.sh/uv/install.sh | sh

# Update
uv self update 

# Sync dependencies
uv sync
```

### ⚠️ Important Security Warning
> This project executes code generated by Large Language Models (LLMs), which can introduce security risks. To prevent the execution of malicious code, we **strongly recommended** running this project in an isolated environment, such as a:
>
> - Docker Container[（Building the Docker environment）](./Dockerfile)
> - Virtual Machine (VM)
> - Sandbox
> - A dedicated physical test machine
>

### Model API Evaluation

#### Important Notes

- **High Token Consumption Warning**: This evaluation framework will incur significant Token consumption. Before starting, please ensure your API account has a sufficient balance.
Reference Case: A single full evaluation of the GLM-4.5-nothinking model consumes approximately 27 million Tokens.
- **Computational and Time Costs**: This is a computationally intensive task. We recommend running it on hardware with comparable or better performance.
Performance Benchmark: On a 32C128G server with unrestricted API concurrency, a full evaluation is estimated to take approximately 1.5 hour.

Note that the resource consumption and evaluation time will gradually increase as more test cases are added.

#### Quick Start
```bash
$ uv run -m sec_code_bench.eval \
            --language_list java \
            --eval_llm_list 'OPENAI::ModelUnderTest::APIKey::Endpoint' \
            --judge_llm_list \
            'OPENAI::ModelNameA::APIKey::Endpoint' \
            'OPENAI::ModelNameB::APIKey::Endpoint' \
            'OPENAI::ModelNameC::APIKey::Endpoint' \
            --benchmark ./datasets/benchmark/java/java.json
```

#### Usage
```
usage: eval.py [-h] --benchmark BENCHMARK [--config CONFIG] [--log_level {DEBUG,INFO,WARNING,ERROR,CRITICAL}] [--log-dir LOG_DIR] --language_list LANGUAGE_LIST [LANGUAGE_LIST ...]
               --eval_llm EVAL_LLM
               --judge_llm_list JUDGE_LLM_LIST [JUDGE_LLM_LIST ...] [--experiment_cycle EXPERIMENT_CYCLE]

SecCodeBench - A Security Benchmark for AI-Generated and -Repaired Code

options:
  -h, --help            show this help message and exit
  --benchmark BENCHMARK
                        Path to the benchmark test file
  --config CONFIG       Configuration file path (default: config.ini)
  --log_level {DEBUG,INFO,WARNING,ERROR,CRITICAL}
                        Logging level (default: INFO)
  --log-dir LOG_DIR     Log directory path (default: ./logs/)
  --language_list LANGUAGE_LIST [LANGUAGE_LIST ...]
                        Benchmark languages, e.g., java, python
  --eval_llm EVAL_LLM.  LLM to benchmark provided as PROVIDER::MODEL::API_KEY::BASE_URL, e.g., OPENAI::gpt-3.5-turbo::your-api-key::https://api.openai.com/v1. Can be specified
                        multiple times to test multiple LLMs.
  --judge_llm_list JUDGE_LLM_LIST [JUDGE_LLM_LIST ...]
                        Judge LLMs provided as PROVIDER::MODEL::API_KEY::BASE_URL, e.g., OPENAI::gpt-3.5-turbo::your-api-key::https://api.openai.com/v1. Can be specified multiple
                        times. Must be odd number for majority voting.
  --experiment_cycle EXPERIMENT_CYCLE
                        Number of experiment cycles for each test case (default: 10)
```

For more configuration options, please refer to `config.ini`。

### Agentic Coding Tool Evaluation

#### Configuration Guide

This guide will help you correctly configure your environment for end-to-end (E2E) automated agentic coding tool evaluation。

1. Supported Environments and Types
  - **Operating System**: The framework is primarily developed and validated on macOS (Apple Silicon).
  - **Supported Agent** Types: VSCode-like editors (IDEs); VSCode Plugins; Command-Line Interface (CLI) tools.

2. Prerequisites
  - **Update to Latest Versions**: Ensure all editors, IDEs, and plugins to be tested are updated to their latest official versions.
  - **Set Display Language**: To ensure accurate UI element location, please set the display language of your editor/IDE to Chinese.
  - **Prepare API Account**: Ensure your configured LLM API account has a sufficient balance to cover the high Token consumption during evaluation.
  - **Authorize Automated Execution**: Pre-authorize the Agent to automatically execute terminal commands within the target application. Settings vary by tool, so please refer to the respective documentation.
  - **Configure Workspace Trust (VSCode Example)**: To allow the Agent to read/write files and execute commands without interruption, disable workspace trust prompts.
    - Open VSCode Settings.
    - Search for Security: Workspace Trust.
    - Uncheck the Security: Workspace Trust: Enabled option to disable trust requirements for all workspaces.

3. Performance and Concurrency Recommendations
 - **CLI Tools**：Support high-concurrency testing mode. The number of concurrent threads can be adjusted based on machine performance.
 - **GUI Applications (IDEs/Plugins)**：Due to UI automation, high concurrency can lead to instability. Based on our tests, we recommend limiting the number of concurrent threads (threads) to 2.
 - **Large-Scale Testing Strategy**：For full-scale evaluations, you can partition the test cases using the -p parameter and run them in parallel across multiple machines to significantly reduce the total evaluation time.。
> Example: We used 5 Apple M2 Mac machines, dividing the test cases into five groups. Each machine ran one group with 2 threads, completing the stable automated generation in about 4 to 5 hours.

4. Supported Agentic Coding Tools and Launch Parameters

| Agentic Coding Tool |Type | -e Launch Parameter|
| :- | :- | :- |
| Github Copilot | vscode-plugins | vscode-copilot | 
| Lingma| vscode-plugins | vscode-lingma|
| CodeBuddy | vscode-plugins | vscode-buddy |
| Comate | vscode-plugins | vscode-zulu |
| Trae | IDE | trae|
| Qoder | IDE | qoder |
| Cursor |IDE | cursor |

#### Quick Start
```bash
$ uv run -m sec_code_bench.e2e \
            --language_list java \
            --judge_llm_list \
            'OPENAI::ModelNameA::APIKey::Endpoint' \
            'OPENAI::ModelNameB::APIKey::Endpoint' \
            'OPENAI::ModelNameC::APIKey::Endpoint' \
            --threads 2 \   # recommend
            -e vscode-lingma \
            --benchmark ./datasets/benchmark/java/java.json
```

#### Usage
```
usage: e2e.py [-h] --benchmark BENCHMARK [--config CONFIG] [--log_level {DEBUG,INFO,WARNING,ERROR,CRITICAL}] [--log-dir LOG_DIR] --language_list LANGUAGE_LIST
              [LANGUAGE_LIST ...] [--judge_llm_list JUDGE_LLM_LIST [JUDGE_LLM_LIST ...]] [--experiment_cycle EXPERIMENT_CYCLE]
              [--editor {IDEType.VSCODE_LINGMA,IDEType.VSCODE_BUDDY,IDEType.VSCODE_ZULU,IDEType.VSCODE_GITHUB_COPILOT,IDEType.LINGMA,IDEType.CURSOR,IDEType.TRAE,IDEType.QODER,IDEType.CodeBuddy,IDEType.CLAUDE_CODE,IDEType.CODEBUDDY_CLI,IDEType.QWEN_CODE,IDEType.CODEX}]
              [--prepare] [--threads THREADS] [--debug] [--prompt PROMPT]

SecCodeBench - Security Evaluation Framework for LLM-generated code

options:
  -h, --help            show this help message and exit
  --benchmark BENCHMARK
                        Path to the benchmark test file
  --config CONFIG       Configuration file path (default: config.ini)
  --log_level {DEBUG,INFO,WARNING,ERROR,CRITICAL}
                        Logging level (default: INFO)
  --log-dir LOG_DIR     Log directory path (default: ./logs/)
  --language_list LANGUAGE_LIST [LANGUAGE_LIST ...]
                        Benchmark languages, e.g., java, python
  --judge_llm_list JUDGE_LLM_LIST [JUDGE_LLM_LIST ...]
                        Judge LLMs provided as PROVIDER::MODEL::API_KEY::BASE_URL, e.g., OPENAI::model-name::your-api-key::https://api.openai.com/v1. Can be specified
                        multiple times. Must be odd number for majority voting.
  --experiment_cycle EXPERIMENT_CYCLE
                        Number of experiment cycles for each test case (default: 10)
  --editor {IDEType.VSCODE_LINGMA,IDEType.VSCODE_BUDDY,IDEType.VSCODE_ZULU,IDEType.VSCODE_GITHUB_COPILOT,IDEType.LINGMA,IDEType.CURSOR,IDEType.TRAE,IDEType.QODER,IDEType.CodeBuddy,IDEType.CLAUDE_CODE,IDEType.CODEBUDDY_CLI,IDEType.QWEN_CODE,IDEType.CODEX}, -e {IDEType.VSCODE_LINGMA,IDEType.VSCODE_BUDDY,IDEType.VSCODE_ZULU,IDEType.VSCODE_GITHUB_COPILOT,IDEType.LINGMA,IDEType.CURSOR,IDEType.TRAE,IDEType.QODER,IDEType.CodeBuddy,IDEType.CLAUDE_CODE,IDEType.CODEBUDDY_CLI,IDEType.QWEN_CODE,IDEType.CODEX}
                        Specify the editor type to be used, default is vscode
  --prepare, -f         Call the prepare method of the editor before execution
  --threads THREADS     Specify the number of worker threads for parallel execution (default: 1)
  --debug               Enable debug mode for application type editors - save debug snapshots on exceptions
  --prompt PROMPT, -p PROMPT
                        Filter testcases: use range like '0-4' for indicesor string for exact/partial key matching (exact match preferred). Empty means all testcases.
```

## 🗺️ Roadmap
We are committed to making `SecCodeBench` a continuously evolving, vibrant security benchmark. Our future work will focus on:
*   **Expanding Java Test Cases**: We will consistently add more Java test cases that reflect real-world scenarios to cover a broader range of CWE categories.
*   **Adding Multi-Language Support**: After strengthening the Java dataset, we plan to support other mainstream languages like Python, C++, and JavaScript.
*   **Community-Driven Development**: We will actively listen to community feedback to iterate on and refine our dataset, ensuring the long-term quality and fairness of the benchmark.

We welcome you to create [Issues](https://github.com/alibaba/sec-code-bench/issues) to discuss new features or propose suggestions!

## Contributors

Thanks to all the developers who have contributed to this project!

<div align="center">
  <span href="[Alibaba Security]" target="_blank" style="margin: 0 15px;">
    <img src="./docs/images/alibaba_security_logo.png" alt="Alibaba Security Logo" height="100"/>
  </span>
  <span href="[Alibaba Cloud Security]" target="_blank" style="margin: 0 15px;">
    <img src="./docs/images/alibaba_cloud_security_logo.png" alt="Alibaba Cloud Security Logo" height="90"/>
  </span>

  <br>

  <span href="[Zhejiang University]" target="_blank" style="margin: 0 15px;">
    <img src="./docs/images/zhejiang_university_logo.png" alt="Zhejiang University Logo" height="100"/>
  </span>
  <span href="[Fudan University]" target="_blank" style="margin: 0 15px;">
    <img src="./docs/images/fudan_university_logo.png" alt="Fudan University Logo" height="100"/>
  </span>
  <span href="[Tsinghua University]" target="_blank" style="margin: 0 15px;">
    <img src="./docs/images/tsinghua_university_logo.png" alt="Tsinghua University Logo" height="100"/>
  </span>
  <span href="[Peking University]" target="_blank" style="margin: 0 15px;">
    <img src="./docs/images/peking_university_logo.png" alt="Peking University Logo" height="100"/>
  </span>
</div>

<br>

## 📄 License

This project is licensed under the [Apache 2.0 license](LICENSE).
