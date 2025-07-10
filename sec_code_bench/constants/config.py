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

import os
import shutil
import sys

from colorama import Fore, Style, init

try:
    from .config_manager import get_config_manager

    _config_manager = get_config_manager()
    _use_config_file = True
except (ImportError, FileNotFoundError):
    _use_config_file = False
    _config_manager = None


def _get_config_value(section: str, key: str, fallback: str) -> str:
    """Get configuration value from .ini file or fallback to hardcoded value"""
    if _use_config_file and _config_manager:
        try:
            return _config_manager.get(section, key, fallback=fallback)
        except Exception:
            return fallback
    return fallback


def _get_config_int(section: str, key: str, fallback: int) -> int:
    """Get configuration value as int from .ini file or fallback to hardcoded value"""
    if _use_config_file and _config_manager:
        try:
            return _config_manager.getint(section, key, fallback=fallback)
        except Exception:
            return fallback
    return fallback


def _get_config_bool(section: str, key: str, fallback: bool) -> bool:
    """Get configuration value as bool from .ini file or fallback to hardcoded value"""
    if _use_config_file and _config_manager:
        try:
            return _config_manager.getboolean(section, key, fallback=fallback)
        except Exception:
            return fallback
    return fallback


# original dataset
benchmark_dir = "datasets/static/dataset"
instruct_prompt_fpath = f"datasets/static/prompts/instruct_prompt.{_get_config_value('DEFAULT', 'locale', 'zh-CN')}.txt"
complete_prompt_fpath = f"datasets/static/prompts/complete_prompt.{_get_config_value('DEFAULT', 'locale', 'zh-CN')}.txt"


# JavaParser JAR package paths
jar_package_dir = "datasets/static/jar_package"
to_resolve_dir = os.path.join(jar_package_dir, "to_resolve")

# Core JAR files for JavaParser
core_jars = {
    "javaparser_core": "javaparser-core-3.27.0.jar",
    "javaparser_symbol_solver": "javaparser-symbol-solver-core-3.27.0.jar",
    "javassist": "javassist-3.30.2-GA.jar",
}

# cached results
cached_dir = "datasets/static/cached"
cached_prompt_dir = os.path.join(cached_dir, "prompts")
cached_generated_dir = os.path.join(cached_dir, "generated")

# model/<mode>/1-level/2-level/component/
cached_generated_model_dir_template = (
    cached_generated_dir
    + "/{model}/{mode}/{primary_type}/{secondary_type}/{component_type}/{testcase_name}"
)
cached_model_result_file_name = "result.txt"
cached_judge_model_result_file_name = "judge_model_result.csv"
cached_statistics_file_name = "statistics.csv"

file_marker = "<filename>"
fim_marker = "<fim_suffix>"
fim_prefix_marker = "<fim_prefix>"


# Default locale
locale = _get_config_value("DEFAULT", "locale", "zh-CN")

# Default thread count
thread_count = _get_config_int("DEFAULT", "thread_count", 2)

# Default retry count
retry_count = _get_config_int("DEFAULT", "retry_count", 3)

# Rate limiting configuration
rate_limit_max_requests = _get_config_int("DEFAULT", "rate_limit_max_requests", 60)
rate_limit_window_seconds = _get_config_int("DEFAULT", "rate_limit_window_seconds", 60)

# Logging level
log_level = _get_config_value("DEFAULT", "log_level", "INFO")

# Checker configuration
enable_syntax_check = _get_config_bool("CHECKERS", "enable_syntax_check", False)
enable_sink_validation = _get_config_bool("CHECKERS", "enable_sink_validation", False)


def reset_cached_output_dir(new_cached_dir: str):
    if os.path.exists(new_cached_dir):
        init(autoreset=True)
        prompt = f"The cached dir {new_cached_dir} exists. {Fore.RED} Do you want to{Style.BRIGHT + Fore.RED} DELETE {Style.RESET_ALL}{Fore.RED}the cached directory? (y/N): {Style.RESET_ALL}"
        reply = input(prompt).strip().lower()
        if reply != "y":
            sys.exit(
                f"Cached dir: {new_cached_dir} already exists, please delete it manually and then restart."
            )
        else:
            shutil.rmtree(new_cached_dir)

    global cached_dir
    global cached_prompt_dir
    global cached_generated_dir
    global cached_generated_model_dir_template

    cached_dir = new_cached_dir
    os.makedirs(cached_dir)

    cached_prompt_dir = os.path.join(cached_dir, "prompts")
    os.makedirs(cached_prompt_dir)
    cached_generated_dir = os.path.join(cached_dir, "generated")
    os.makedirs(cached_generated_dir)

    # model/<mode>/1-level/2-level/component/
    cached_generated_model_dir_template = (
        cached_generated_dir
        + "/{model}/{mode}/{primary_type}/{secondary_type}/{component_type}/{testcase_name}"
    )
