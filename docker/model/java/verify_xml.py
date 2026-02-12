#!/usr/bin/env python3
"""
独立的 XML 格式代码验证脚本
与 C-Verifier 的 verify.py 架构保持一致

用法: python verify_xml.py <testcase> <work_dir> <code> <prompt_path> <judge_llm_list_json> <locale>
"""

import sys
import json
import re
import shutil
from pathlib import Path

# 添加当前目录到 Python 路径，以便导入项目模块
sys.path.insert(0, str(Path(__file__).parent.absolute()))

from scripts.log_sdk import logger
from verifier.llm.llm_manager import LLMManager
from verifier.tester.function import FunctionTester
from verifier.tester.security import SecurityTester
from verifier.utils.fdisk_utils import save_file
from verifier.utils.testcase_util import load_testcase
from verifier import basic_init_llm


def extract_xml_from_response(response: str) -> str:
    """
    从 LLM 响应中提取 XML - 与 C-Verifier 一致

    Args:
        response: LLM 的原始响应

    Returns:
        提取的 XML 字符串
    """
    # 先尝试从代码块中提取（常见情况）
    code_block_match = re.search(r'```(?:xml)?\s*(<result>.*?</result>)\s*```', response, re.DOTALL)
    if code_block_match:
        return code_block_match.group(1)

    # 否则直接匹配（与 C-Verifier 一致）
    pattern = r"<result>.*?</result>"
    matched = re.findall(pattern, response, re.DOTALL | re.MULTILINE)
    if matched:
        return matched[-1]  # 返回最后一个匹配

    return response


def xml_to_dict_(element):
    """将 XML 元素递归转换为字典"""
    children = list(element)
    text = element.text.strip() if element.text and element.text.strip() else None

    if not children:
        return text

    result = {}
    for child in children:
        child_data = xml_to_dict_(child)
        if child.tag not in result:
            result[child.tag] = []
        result[child.tag].append(child_data)

    return result


def fix_xml(xml_string, tag_list=None):
    """修复XML字符串，为指定标签添加CDATA包装"""
    if tag_list:
        for tag in tag_list:
            fixed_xml = re.sub(
                f'<{tag}>(.*?)</{tag}>',
                fr'<{tag}><![CDATA[\1]]></{tag}>',
                xml_string,
                flags=re.DOTALL
            )
            xml_string = fixed_xml
    return xml_string


def xml_to_dict(xml_string, tags=None):
    """将 XML 字符串转换为字典结构"""
    import xml.etree.ElementTree as ET

    xml_string = fix_xml(xml_string, tags)
    try:
        root = ET.fromstring(xml_string)
        result = {root.tag: xml_to_dict_(root)}
        return result
    except ET.ParseError as e:
        raise Exception(f"xml parse error: {e}")


def parse_and_write_code(response: str, code_dir: Path) -> bool:
    """
    解析LLM响应并写入文件

    注意：这里的 xml_string 已经由 Python 解释器处理过转义字符，
    所以不需要手动处理 \\n 转换
    """
    try:
        # 1. 从响应中提取 XML
        extracted_xml = extract_xml_from_response(response)
        logger.info(f"Extracted XML length: {len(extracted_xml)} characters")

        # 2. 解析 XML（不需要手动处理转义，Python 解释器已经处理）
        result = xml_to_dict(extracted_xml)
        if not isinstance(result, dict):
            logger.error(f"xml_to_dict returned non-dict type: {type(result)}")
            return False

        codes = result.get("result", {})
        if not isinstance(codes, dict):
            logger.error(f"Result is not a dict: {codes}")
            return False

        code_list = codes.get("code", [])
        if not isinstance(code_list, list):
            logger.error(f"Code is not a list: {code_list}")
            return False

        if not code_list:
            logger.error("Code list is empty")
            return False

        # 3. 处理每个代码项
        for code_item in code_list:
            if not isinstance(code_item, dict):
                logger.error(f"Code item is not a dict: {code_item}")
                continue

            path_list = code_item.get("path", [])
            content_list = code_item.get("content", [])

            if not path_list or not content_list:
                logger.error(f"Missing path or content in code item")
                continue

            path = path_list[0] if isinstance(path_list, list) else path_list
            code_content = content_list[0] if isinstance(content_list, list) else content_list

            if not path or not code_content:
                logger.error(f"Empty path or content")
                continue

            # 清理路径中的空白字符
            path = path.strip()

            try:
                # 确保路径以 src/ 开头
                if not path.startswith("src/"):
                    path = f"src/{path}"

                full_path = code_dir / path
                full_path.parent.mkdir(parents=True, exist_ok=True)
                save_file(full_path, code_content)
                logger.info(f"Successfully wrote code to {path}")
            except Exception as e:
                logger.error(f"Failed to write code to {path}: {e}")
                return False

        return True

    except Exception as e:
        logger.error(f"Error processing XML code: {e}")
        logger.error(f"XML content preview: {response[:500]}...")
        return False


def init_llm_manager(judge_llm_list: list[str]) -> LLMManager:
    """初始化LLMManager"""
    import argparse

    args = argparse.Namespace()
    args.judge_llm_list = judge_llm_list

    return basic_init_llm(args, logger)


def run_function_eval(testcase, code_dir, llm_manager, judge_list, locale):
    """在独立线程中运行功能测试"""
    import threading
    import queue

    result_queue = queue.Queue()
    exception_queue = queue.Queue()

    def run_in_thread():
        try:
            result = FunctionTester.function_eval(
                testcase=testcase,
                code_dir=code_dir,
                llm_manager=llm_manager,
                judge_list=judge_list,
                locale=locale
            )
            result_queue.put(result)
        except Exception as e:
            exception_queue.put(e)

    thread = threading.Thread(target=run_in_thread, daemon=False)
    thread.start()
    thread.join(timeout=600)

    if not exception_queue.empty():
        raise exception_queue.get()

    if not result_queue.empty():
        return result_queue.get()

    raise TimeoutError("Function evaluation timeout")


def run_security_eval(testcase, code_dir, llm_manager, judge_list, locale):
    """在独立线程中运行安全测试"""
    import threading
    import queue

    result_queue = queue.Queue()
    exception_queue = queue.Queue()

    def run_in_thread():
        try:
            result = SecurityTester.security_eval_sync(
                testcase=testcase,
                code_dir=code_dir,
                llm_manager=llm_manager,
                judge_list=judge_list,
                locale=locale
            )
            result_queue.put(result)
        except Exception as e:
            exception_queue.put(e)

    thread = threading.Thread(target=run_in_thread, daemon=False)
    thread.start()
    thread.join(timeout=600)

    if not exception_queue.empty():
        raise exception_queue.get()

    if not result_queue.empty():
        return result_queue.get()

    raise TimeoutError("Security evaluation timeout")


def main():
    """主函数 - 与 C-Verifier 的 verify.py 结构一致"""
    if len(sys.argv) < 7:
        print("Usage: python verify_xml.py <testcase> <work_dir> <code> <prompt_path> <judge_llm_list_json> <locale>")
        sys.exit(1)

    testcase_id = sys.argv[1]
    work_dir = Path(sys.argv[2])
    code = sys.argv[3]  # Python 解释器已自动处理转义字符
    prompt_path = sys.argv[4]
    judge_llm_list_json = sys.argv[5]
    locale = sys.argv[6]

    # 解析 judge_llm_list
    judge_llm_list = json.loads(judge_llm_list_json) if judge_llm_list_json else []

    result = {
        "stdout": "",
        "stderr": "",
        "test_result": {
            "functional_result": {
                "total_tests": 0,
                "total_failures": 0,
                "total_errors": 0,
                "total_skipped": 0
            },
            "security_result": {
                "total_tests": 0,
                "total_failures": 0,
                "total_errors": 0,
                "total_skipped": 0
            }
        },
        "succ_rate": -1,
    }

    try:
        # 1. 加载 testcase
        logger.info(f"Loading testcase: {testcase_id}")
        testcase_obj = load_testcase(testcase_id)

        # 2. 选择 scenario
        scenario = None
        if prompt_path == "default":
            if testcase_obj.scenarios:
                scenario = testcase_obj.scenarios[0]
                logger.info(f"Using default scenario: {scenario.value}")
            else:
                raise ValueError(f"No scenarios available for testcase: {testcase_id}")
        else:
            for s in testcase_obj.scenarios:
                if s.value == prompt_path:
                    scenario = s
                    break
            if scenario is None:
                available_scenarios = [s.value for s in testcase_obj.scenarios]
                raise ValueError(
                    f"Invalid prompt_path '{prompt_path}'. "
                    f"Available scenarios: {available_scenarios}"
                )
            logger.info(f"Using scenario: {scenario.value}")

        # 3. 初始化 LLMManager
        if not judge_llm_list:
            error_msg = "judge_llm_list is required"
            logger.error(error_msg)
            result["stderr"] = error_msg
        else:
            logger.info(f"Initializing LLMManager with {len(judge_llm_list)} models")
            llm_manager = init_llm_manager(judge_llm_list)

            # 4. 准备 code_dir
            logger.info("Preparing code directory...")
            current_dir = Path(__file__).parent.absolute()
            code_template_dir = (
                current_dir
                / "datasets/templates"
                / testcase_obj.language.value
                / testcase_obj.template
            )

            if not code_template_dir.exists():
                raise ValueError(f"Template directory not found: {code_template_dir}")

            code_dir = work_dir / f"{testcase_obj.prompt}_{scenario.value}_xml_verify"
            shutil.copytree(str(code_template_dir), str(code_dir))

            # 5. 解析并写入代码
            logger.info("Parsing and writing code...")
            if not parse_and_write_code(code, code_dir):
                error_msg = "Failed to parse XML code"
                logger.error(error_msg)
                result["stderr"] = error_msg
            else:
                # 6. 执行功能测试
                logger.info("Running function evaluation...")
                fun_result = run_function_eval(
                    testcase_obj,
                    code_dir,
                    llm_manager,
                    judge_llm_list,
                    locale
                )

                result["test_result"]["functional_result"] = {
                    "total_tests": fun_result.tests,
                    "total_failures": fun_result.failures,
                    "total_errors": fun_result.errors,
                    "total_skipped": fun_result.skipped,
                }

                function_stdout = fun_result.stdout or ""
                function_stderr = fun_result.stderr or ""
                if fun_result.error_message:
                    if function_stderr:
                        function_stderr += f"\n\n[Error Message]\n{fun_result.error_message}"
                    else:
                        function_stderr = f"[Error Message]\n{fun_result.error_message}"

                # 7. 如果功能测试通过，执行安全测试
                security_stdout = ""
                security_stderr = ""

                if fun_result.success and fun_result.if_pass():
                    logger.info("Running security evaluation...")
                    sec_result = run_security_eval(
                        testcase_obj,
                        code_dir,
                        llm_manager,
                        judge_llm_list,
                        locale
                    )

                    result["test_result"]["security_result"] = {
                        "total_tests": sec_result.tests,
                        "total_failures": sec_result.failures,
                        "total_errors": sec_result.errors,
                        "total_skipped": sec_result.skipped,
                    }

                    security_stdout = sec_result.stdout or ""
                    security_stderr = sec_result.stderr or ""
                    if sec_result.error_message:
                        if security_stderr:
                            security_stderr += f"\n\n[Internal Error]\n{sec_result.error_message}"
                        else:
                            security_stderr = f"[Internal Error]\n{sec_result.error_message}"
                else:
                    logger.info("Function test failed, skipping security evaluation")

                # 合并输出
                combined_stdout = f"=== Function Test Output ===\n{function_stdout}\n\n=== Security Test Output ===\n{security_stdout}"
                combined_stderr = f"=== Function Test Errors ===\n{function_stderr}\n\n=== Security Test Errors ===\n{security_stderr}"

                result["stdout"] = combined_stdout.strip()
                result["stderr"] = combined_stderr.strip()
                result["function_stdout"] = function_stdout
                result["function_stderr"] = function_stderr
                result["security_stdout"] = security_stdout
                result["security_stderr"] = security_stderr

                # 8. 计算成功率
                func_result = result["test_result"]["functional_result"]
                sec_result = result["test_result"]["security_result"]

                total_tests = func_result["total_tests"] + sec_result["total_tests"]
                if total_tests > 0:
                    total_passed = (
                        (func_result["total_tests"] - func_result["total_failures"] -
                         func_result["total_errors"] - func_result["total_skipped"]) +
                        (sec_result["total_tests"] - sec_result["total_failures"] -
                         sec_result["total_errors"] - sec_result["total_skipped"])
                    )
                    result["succ_rate"] = total_passed / total_tests
                else:
                    result["succ_rate"] = 0.0

    except Exception as e:
        logger.error(f"Error in verify_xml: {e}", exc_info=True)
        result["stderr"] = str(e)
        result["succ_rate"] = 0.0

    # 写入结果文件（与 C-Verifier 一致）
    result_file = work_dir / "result.json"
    with open(result_file, "w") as f:
        json.dump(result, f, indent=2)

    logger.info(f"Result written to {result_file}")


if __name__ == "__main__":
    main()
