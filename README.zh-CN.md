# SecCodeBench
![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)
![Language](https://img.shields.io/badge/language-Python3-orange.svg)
![Status](https://img.shields.io/badge/status-active-brightgreen.svg)

<div align="middle">

[**English**](./README.md) · **简体中文**

</div>

SecCodeBench是一个由阿里巴巴集团与浙江大学网络空间安全学院、复旦大学、清华大学网络科学与网络空间研究院、北京大学共建的、专注于评估大语言模型（LLM）生成代码安全性的基准测试套件。


## 📖 概述

随着以大语言模型（LLM）为核心的辅助编程工具的普及，一个至关重要的问题浮出水面：**AI 生成的代码安全吗？** 为了回答这个问题，一个高质量、高精度的评测基准至关重要。

然而，社区现有的安全评测基准在两个核心维度上存在显著的局限性，这使得它们难以真实反映模型或Agent的安全编码能力：

*   **测试用例质量参差不齐**：许多数据集严重依赖自动化生成和简单过滤，缺乏人工的深度参与。这导致了 **(a) 数据分布失衡**，大量低优先级的安全问题占据主导，无法有效衡量模型在关键漏洞上的表现；**(b) 测试用例脱离实际**，部分测试用例可能包含不当的诱导性提示，影响了评估的公正性；

*   **评估方法过于单一且精度不足**：现有的评估方法大多依赖于简单的正则表达式或代码检测工具，这导致了两个严重问题：它们难以准确识别**语法或语义复杂**的代码变体，并且完全忽略了必须通过**真实运行**才能验证的动态漏洞。

为解决上述问题，我们推出了 `SecCodeBench`，一个具备**高质量数据**和**科学评估方法**的基准测试套件：

*   **在数据构建上**，`SecCodeBench`的398个测试用例不仅来源于对15万个真实GitHub项目的深度扫描，更由众多安全专家参与构建，每个用例均经过双人评审确认。我们确保了数据集在46个细分漏洞类型上的**均匀分布**，并移除了所有可能产生不当引导的模糊表述，尽力保证了评测的公平与真实；

*   **在评估方法上**，我们采用了**动态执行与多种静态分析相结合的评估策略**。不仅通过可运行的POC来验证模型生成代码的安全性，更**引入大模型作为评委（LLM-as-a-Judge）**，并为其注入了丰富的安全领域知识。实践证明，在处理复杂的、需要深度语义理解的漏洞时，大模型的评估表现远超传统的规则匹配方法。

更多信息请参考[技术报告](docs/SecCodeBench_Technical_Report.pdf).

## ✨ 核心特点

*   **为安全而生**: 不同于关注功能性的基准，`SecCodeBench` 专注于 LLM 安全编码能力的测评；
*   **覆盖两大主流场景**: 针对性地为 `Instruct`（指令驱动编码）和 `Autocomplete/FillInTheMiddle`（代码自动补全）这两种最核心的 LLM 编程场景设计了不同的评测范式；
*   **混合评估策略**: 考虑到代码不可执行不代表不安全，因此结合了动态运行测试与多种静态分析方法，以全面评估其安全性；
*   **源于真实世界的数据集**: 静态测试集源于对 GitHub 上约15万个真实代码库的扫描，并经过安全专家的逐一审查；动态测试集源自众多安全专家的精心构造，确保了评测的真实性和挑战性。

## 🔬 评测方法论

我们根据 LLM 辅助编程的两种核心场景，设计了不同的评测流水线。

### Instruct

即基于各类旗舰或编码模型，结合 Prompt Engineering，以问答形式，结合多轮思考以及工具调用形式生成代码的场景，我们采用“动态+静态”的混合评测模式：

*   **动态运行测试**: 基于安全工程师的实战经验，构造了18个可实际运行的漏洞利用场景，用于检测代码在真实运行环境中的安全表现；
*   **静态正则匹配**: 使用高精度的正则表达式快速检测已知的、模式明显的漏洞；
*   **LLM-as-a-Judge**: 利用大模型自身进行安全评审。在当前的评估场景下，大模型和专业引擎相比差距小于5%。并且在需要深度语义理解的漏洞类型上，大模型的表现甚至优于专业引擎。

### Autocomplete

即基于微调过的代码补全模型生成代码片段（通常无法直接编译运行）的场景，我们采用纯静态的评测方法：

*   **静态正则匹配**
*   **LLM-as-a-Judge**

## 📊 数据集构成

`SecCodeBench` 的数据集经过安全专家的精心设计与筛选，确保了高质量和高覆盖率。**测试用例主要遵循业界广泛认可的通用缺陷枚举（CWE）标准进行分类。在当前版本中，我们专注于 Java 语言**，并根据大模型辅助编程的两种核心应用场景——`Instruct` 和 `Autocomplete`——来组织我们的数据集

| 场景 | 评估方法 | 数据来源 | 漏洞/组件类型 | 测试用例数 |
| --- | --- | --- | --- | --- |
| **Autocomplete** | 静态评估 | 扫描约15万GitHub Java仓库 | 46 | 398 |
| **Instruct** | 静态评估 | 扫描约15万GitHub Java仓库 | 46 | 398 |
| **Instruct** | 动态评估 | 安全专家人工审核构造 | 17 | 18 |

> *注：静态测试集中的每个漏洞类型的用例（约10个）均来自不同的代码仓库，以保证数据的多样性。

## 🗺️ 路线图
我们致力于将 `SecCodeBench` 打造成一个持续演进、充满活力的安全基准，未来的工作将围绕以下几个方向展开：
*   **持续扩充 Java 用例**：我们将不断增加更多、更贴近真实业务场景的 Java 测试用例，以覆盖更广泛的 CWE 类别；
*   **扩展多语言支持**：在完善 Java 数据集的基础上，我们计划逐步支持其他主流编程语言，如 Python, C++, JavaScript 等；
*   **社区驱动的迭代与修复**：我们将积极听取社区的反馈，持续迭代和修正数据集中可能存在的问题，以保证基准的长期高质量与公正性。

我们非常欢迎您通过 [创建 Issue](https://github.com/alibaba/sec-code-bench/issues) 来讨论新功能或提出建议！

## 🚀 如何使用 (Getting Started)

为了保证评测结果的可复现性，我们强烈建议您使用本项目的**正式发布版本 (Official Releases)**，而不是直接从 `main` 分支拉取。

### 获取仓库
您可以通过以下方式以下命令克隆特定版本的代码和数据

```bash
# 克隆整个仓库
git clone https://github.com/alibaba/sec-code-bench.git
cd sec-code-bench

# 切换到你需要的版本标签
git checkout v1.0.0
```

### 环境配置
- Python: 3.11 或更高版本
- Java: JDK 17
- Maven: 3.6+ 或更高版本 (用于构建和管理Java项目)

安装 uv（如尚未安装），用于项目管理与依赖同步
```bash
# 安装
curl -LsSf https://astral.sh/uv/install.sh | sh

# 更新
uv self update 

# 同步依赖
uv sync
 ```

### 运行

> #### ⚠️ 重要安全警告
>
> 本项目会执行由大语言模型（LLM）生成的代码，这可能带来未知的安全风险。为防止潜在的恶意代码执行，**强烈建议**在隔离的环境中运行本项目，例如：
>
> - Docker 容器 [（构建docker环境）](./Dockerfile)
> - 虚拟机 (VM)
> - 专用的沙箱环境

#### 1. 静态检测
本评测工具也支持通过静态分析，从 `instruct` 与 `autocomplete` 两种场景下衡量大语言模型（LLM）的安全编程能力。启动方法如下：

```bash
# 请在 .env 文件中指定需评测的 LLM 及其对应的 API key 和 endpoint，环境变量名称应以模型名大写开头（例如：GPT-4_API_KEY, GPT-4_ENDPOINT）。
$ cp .env.example .env
$ vim .env

# 示例&启动:
$ uv run sec_code_bench/eval_static.py --help
  usage: eval_static.py [-h] [--models MODEL_LIST] --eval-type {instruction,completion} [--language LANGUAGE] [--vuln-file VULNERABILITY_FILE] [--cached-dir CACHED_DIR]

  SAST SEC-LLM Coding Evaluation System

  options:
    -h, --help            show this help message and exit
    --models MODEL_LIST   Comma-separated list of models to evaluate (e.g., GPT-4,CLAUDE-3), also can be set in .env file
    --eval-type {instruction,completion}
                          LLM SecurityEvaluation type
    --language LANGUAGE   Language to evaluate (default: java)
    --vuln-file VULNERABILITY_FILE, --eval-vulnerability-file VULNERABILITY_FILE
                          Path to YAML configuration file for vulnerability types to evaluate (default: datasets/static/vulnerability_schema.yaml)
    --cached-dir CACHED_DIR, --output-cached-dir CACHED_DIR
                          Cached Output directory for results (default: datasets/static/cached)

          Examples:
              # Quick start
              uv run sec_code_bench/eval_static.py --eval-type instruction

              # Custom evaluation with more configs
              uv run sec_code_bench/eval_static.py --eval-type instruction --models qwen3-235b-a22b,qwen-coder-plus --language java --vuln-file datasets/static/vulnerability_schema.yaml --cached-dir datasets/static/cached
```

更多配置信息请参见 `config.ini`和`.env.example`。

#### 2. 动态检测

支持通过动态运行，验证大语言模型（LLM）在 `instruct` 场景下的安全编程能力。启动方法如下：
```Python3
uv run -m sec_code_bench.eval_dynamic \
    --benchlanguage java \
    --benchmark ./datasets/runnable/benchmark/java/java.json \
    --llm-under-test "OPENAI::<ModelName>::<APIKey>::<URL>"
```

## 贡献者

感谢所有为本项目作出贡献的开发者们！

<div align="center">
  <span href="[Alibaba Security]" target="_blank" style="margin: 0 15px;">
    <img src="./docs/assets/figures/alibaba_security_logo.png" alt="Alibaba Security Logo" height="100"/>
  </span>
  <span href="[Alibaba Cloud Security]" target="_blank" style="margin: 0 15px;">
    <img src="./docs/assets/figures/alibaba_cloud_security_logo.png" alt="Alibaba Cloud Security Logo" height="90"/>
  </span>

  <br>

  <span href="[Zhejiang University]" target="_blank" style="margin: 0 15px;">
    <img src="./docs/assets/figures/zhejiang_university_logo.png" alt="Zhejiang University Logo" height="100"/>
  </span>
  <span href="[Fudan University]" target="_blank" style="margin: 0 15px;">
    <img src="./docs/assets/figures/fudan_university_logo.png" alt="Fudan University Logo" height="100"/>
  </span>
  <span href="[Tsinghua University]" target="_blank" style="margin: 0 15px;">
    <img src="./docs/assets/figures/tsinghua_university_logo.png" alt="Tsinghua University Logo" height="100"/>
  </span>
  <span href="[Peking University]" target="_blank" style="margin: 0 15px;">
    <img src="./docs/assets/figures/peking_university_logo.png" alt="Peking University Logo" height="100"/>
  </span>
</div>

<br>

## 📄 许可证

本项目采用 [Apache 2.0 license](LICENSE) 开源许可证。

