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

*   **Dataset**: We ensure the authenticity and diversity of our test cases. Most of the cases are based on **anonymized, real-world historical vulnerabilities from within Alibaba** and are presented as complete, runnable projects rather than mere code snippets. Each test case is uniquely defined by four attributes: **(Functional Requirements, Programming Language, Third-Party Libraries, Function Interface)**. Currently, it includes **98 test cases** across 5 programming languages (Java: 53, C/C++: 15, Python: 13, Go: 13, Node.js: 4), covering 22 CWE types, adapted into four testing modes: Code Generation (native/security-aware) and Code Fix (native/security-aware). Each test case is crafted by a team of senior security experts and undergoes a rigorous three-person peer review. Furthermore, all cases have been subjected to multiple rounds of empirical testing and fine-tuning across more than ten models to ensure their fairness and challenge.

*   **Evaluation**: We have established a **multi-stage, high-precision evaluation process**. This process is governed by a **"Functionality-First" principle**, where generated code must first pass all functional tests to qualify for security assessment. The security evaluation employs a layered strategy: it **prioritizes dynamic execution-based validation using Proof-of-Concept (PoC) exploits** to ensure objective and reliable results. For complex scenarios not coverable by dynamic execution, we introduce an LLM-as-a-Judge infused with domain-specific security knowledge. The final score is a weighted sum of the pass@1 results, where the weights holistically consider factors such as the **test scenario** (with a 4:1 ratio for native vs. security-aware modes) and a combined metric of **vulnerability prevalence and severity** (assigned weights of 4, 2, and 1 for critical, high, and medium tiers, respectively). This sophisticated scoring mechanism is designed to provide a more authentic reflection of the model's comprehensive security capabilities.

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
git checkout v2.2.0
```

### Environment Setup
- Python: 3.12 or higher
- Docker: 24.0 or higher

Install uv (if not already installed) for project management and dependency synchronization:
```bash
# Install uv
curl -LsSf https://astral.sh/uv/install.sh | sh

# Update
uv self update 

# Sync dependencies
uv sync
```

### Model API Evaluation

#### Important Notes

- **High Token Consumption Warning**: This evaluation framework will incur significant Token consumption. Before starting, please ensure your API account has a sufficient balance.
Reference Case: A single full evaluation of the DeepSeek V3.2 model consumes approximately 22 million tokens in thinking mode, compared to roughly 12 million tokens in non-thinking mode.
- **Computational and Time Costs**: This is a computationally intensive task. We recommend running it on hardware with comparable or better performance.
Performance Benchmark: On a 32C128G server with unrestricted API concurrency, a full evaluation is estimated to take approximately 3 hour.

Note that the resource consumption and evaluation time will gradually increase as more test cases are added.

#### Quick Start

**Step 1: Configure Parameters**

Copy the example configuration file and modify it with your settings:

```bash
cp config.example.yaml config.yaml
```

Edit `config.yaml` to configure the following fields:

| Field | Description |
| :---- | :---------- |
| `lang_configs` | Language configurations for evaluation. Each entry specifies: |
| `lang_configs[].language` | Programming language to evaluate (e.g., `java`, `python`, `cpp`, `go`, `nodejs`) |
| `lang_configs[].benchmark` | Path to the benchmark JSON file (e.g., `./datasets/benchmark/java/java.json`) |
| `eval_llm` | The LLM model to be evaluated |
| `eval_llm.provider` | LLM provider type (e.g., `OPENAI` for OpenAI-compatible APIs) |
| `eval_llm.model` | Model name to evaluate (e.g., `gpt-4`, `qwen-plus`) |
| `eval_llm.api_key` | API key for authentication |
| `eval_llm.endpoint` | API endpoint URL (e.g., `https://api.openai.com/v1`) |
| `judge_llms` | Judge LLMs for security evaluation. **Must be an odd number (1, 3, 5, etc.) for majority voting.** Each entry has the same fields as `eval_llm`. |
| `experiment.cycle` | Number of experiment cycles for each test case (default: 10) |
| `experiment.parameters` | Optional JSON string of parameters to pass to LLM API calls (e.g., `'{"enable_thinking": true}'`) |
| `experiment.rpm_limit` | Optional RPM (Requests Per Minute) limit for the evaluated LLM models (default: 60) |
| `directories.container_result` | Path inside container for results (default: `/dockershare`). When using Docker, set host dir via env `LOCAL_RESULT_DIR`. |

**Step 2: (Optional) Modify System Configuration**

If needed, you can modify `system_config.yaml` to adjust:
- `category_weights`: Score weights for different severity levels (low, medium, high, critical)
- `scenario_weights`: Weights for different test scenarios (gen, gen-hints, fix, fix-hints)
- `languages_need_llm_judges`: Languages that require LLM judges

In most cases, you don't need to modify this file.

**Step 3: Start Verifiers**

Start the shared verifier services (only need to start once, can be reused across multiple evaluations):

```bash
docker compose -f docker-compose-verifiers.yml up -d --build
```

Wait for all verifiers to become healthy:

```bash
docker compose -f docker-compose-verifiers.yml ps
```

**Step 4: Run Evaluation**

Run the evaluation:

```bash
docker compose -f docker-compose-eval.yml up -d
```

You can monitor the evaluation progress by checking the logs:

```bash
docker compose -f docker-compose-eval.yml logs -f
```

**Completion Indicator**: The evaluation is complete when a `finish` file is generated in the output directory at `{result_dir}/finish`, where `result_dir` is under the directory set by `LOCAL_RESULT_DIR` (Docker) or `--log-dir` (native run).

#### Running Multiple Evaluations in Parallel

You can run multiple evaluations simultaneously by using different project names and config files. All evaluations share the same verifier containers.

Create separate config files for each model:

```bash
cp config.example.yaml config-gpt4.yaml
cp config.example.yaml config-claude.yaml
# Edit each file with different model settings
```

Run evaluations in parallel using different project names (`-p`):

```bash
# Terminal 1
CONFIG_FILE=./config-gpt4.yaml docker compose -f docker-compose-eval.yml -p eval-gpt4 up -d

# Terminal 2
CONFIG_FILE=./config-claude.yaml docker compose -f docker-compose-eval.yml -p eval-claude up -d
```


#### Stop Verifiers

When all evaluations are complete, stop the verifier services:

```bash
docker compose -f docker-compose-verifiers.yml down
```

Results are saved under the directory set by `LOCAL_RESULT_DIR` when using Docker (e.g. `LOCAL_RESULT_DIR=/path/to/results docker compose ...`), or by `--log-dir` when running natively.


### Agentic Coding Tool Evaluation

#### Supported Tools

| Agentic Coding Tool | Type | `--editor` Parameter |
| :------------------ | :--- | :------------------- |
| Claude Code         | CLI  | `claude-code`        |
| Qwen Code           | CLI  | `qwen-code`          |
| Codex               | CLI  | `codex`              |
| Gemini CLI          | CLI  | `gemini`             |
| Cursor CLI          | CLI  | `cursor`             |

#### Prerequisites

- **Update to Latest Versions**: Ensure all CLI tools to be tested are updated to their latest official versions.
- **Prepare API Account**: Ensure your configured LLM API account has a sufficient balance to cover the high Token consumption during evaluation.
- **Authorize Automated Execution**: Pre-authorize the CLI tool to automatically execute terminal commands. Settings vary by tool, so please refer to the respective documentation.

#### Performance and Concurrency Recommendations

- **CLI Tools**: Support high-concurrency testing mode. The number of concurrent threads can be adjusted based on machine performance.
- **Large-Scale Testing Strategy**: For full-scale evaluations, you can partition the test cases using the `-p` parameter and run them in parallel across multiple machines to significantly reduce the total evaluation time.

#### Quick Start

**Step 1: Start Verifier Services**

Start the language verifier containers using Docker Compose:

```bash
docker compose -f docker-compose-verifiers.yml up -d --build
```

This will start verifier services for all supported languages (C/C++, Python, Go, Node.js, Java) with port mappings for local access.

**Step 2: Run E2E Evaluation**

Execute the evaluation command:

```bash
uv run -m sec_code_bench.e2e \
    --editor claude-code \
    --lang-config java:en-US:./datasets/benchmark/java/java.json \
    --lang-config go:en-US:./datasets/benchmark/go/go.json \
    --lang-config cpp:en-US:./datasets/benchmark/cpp/c.json \
    --lang-config python:en-US:./datasets/benchmark/python/python.json \
    --lang-config nodejs:en-US:./datasets/benchmark/nodejs/nodejs.json \
    --judge-llm-list \
    'OPENAI::judge-model-1::your-api-key::https://api.openai.com/v1' \
    'OPENAI::judge-model-2::your-api-key::https://api.openai.com/v1' \
    'OPENAI::judge-model-3::your-api-key::https://api.openai.com/v1' \
    --threads 2 \
    --experiment-cycle 1
```

**Completion Indicator**: The evaluation is complete when a `finish` file is generated in the output directory at `{result_dir}/{model_name}/{date}/{time}/finish`.

**Step 3: Stop Verifier Services (when done)**

Stop the Docker containers to free up resources:

```bash
docker compose -f docker-compose-verifiers.yml down
```

#### Command Reference

| Argument | Description |
| :------- | :---------- |
| `--editor`, `-e` | **(Required)** Specify the CLI tool to evaluate (e.g., `claude-code`, `qwen-code`) |
| `--lang-config` | **(Required)** Per-language configuration in format `language:locale:benchmark_path`. Can be specified multiple times for multi-language evaluation. Example: `java:en-US:./datasets/benchmark/java/java.json` |
| `--judge-llm-list` | Judge LLMs provided as `PROVIDER::MODEL::API_KEY::BASE_URL`. Can be specified multiple times. **Must be odd number for majority voting.** |
| `--experiment-cycle` | Number of experiment cycles for each test case (default: 10) |
| `--threads` | Number of worker threads for parallel execution (default: 1) |
| `--batch-size` | Batch size for processing test cases (default: 15) |
| `--prompt`, `-p` | Filter testcases: use range like `0-4` for indices or string for exact/partial key matching. Empty means all testcases. |
| `--prepare`, `-f` | Call the prepare method of the editor before execution |
| `--debug` | Enable debug mode - save debug snapshots on exceptions |
| `--log-level` | Logging level: `DEBUG`, `INFO`, `WARNING`, `ERROR`, `CRITICAL` (default: `INFO`) |
| `--log-dir` | Log directory path (default: `./logs/`) |

## 🗺️ Roadmap
We are committed to making `SecCodeBench` a continuously evolving, vibrant security benchmark. We welcome you to create [Issues](https://github.com/alibaba/sec-code-bench/issues) to discuss new features or propose suggestions!

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
