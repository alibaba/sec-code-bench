import json
import subprocess
import glob
import re
import time
import uuid
import shutil
import os

from code_security_verifier.log_sdk import logger
from code_security_verifier.xml_utils import xml_to_dict


class TestCaseRunner:

    def __init__(self, testcase_path, prompt_path="default", code_dir=None, log_dir=None):
        self.testcase_path = testcase_path
        self.testcase_name = testcase_path.split("/")[-1]
        self.prompt_path = prompt_path
        if not code_dir:
            code_dir = f"/tmp/java_runs/{str(uuid.uuid4())}"
        logger.info(f"testcase_path: {testcase_path} {code_dir}")
        shutil.copytree(testcase_path, code_dir)
        self.code_dir = code_dir
        if not log_dir:
            self.log_dir = os.path.join(os.path.dirname(__file__), "logs")
        else:
            self.log_dir = log_dir
        self.code = ""

        try:
            self.clean_target_file()
        except Exception as e:
            logger.error("clean_target_file_error ", exc_info=e)

    def clean_target_file(self):
        generate_md_pattern = os.path.join(self.code_dir, "**/", "generate.md")
        generate_md_list = glob.glob(generate_md_pattern, recursive=True)
        if len(generate_md_list) == 1:
            generate_md = generate_md_list[0]
            with open(generate_md, "r") as f:
                generate_md = f.read()
            target_file_path = re.search("输出`(.*)`中完整", generate_md).group(1)
            os.remove(os.path.join(self.code_dir, target_file_path))

    def run_test(self):
        import time
        # Initialize variables to capture partial results in case of exceptions
        function_result = None
        security_result = None
        function_test_duration = 0
        security_test_duration = 0

        try:
            start_time = time.time()
            logger.info(f"code_dir: {self.code_dir}")

            # 执行 npm test - 功能测试
            function_test_start = time.time()
            print("开始执行功能测试...")
            logger.info("开始执行功能测试...")
            function_testcase = os.path.basename(self.code_dir)
            function_result = subprocess.run(
                ['npx', 'jest', f'{function_testcase}/test/function.test.js', '--verbose'],
                # ['npm', 'run', 'test:function'],
                cwd=os.path.join(self.code_dir, ".."),
                capture_output=True,
                text=True,
                timeout=60  # 60秒超时
            )
            function_test_duration = time.time() - function_test_start
            print(f"功能测试完成，耗时: {function_test_duration:.2f}秒")
            logger.info(f"功能测试完成，耗时: {function_test_duration:.2f}秒")

            # 执行 npm test - 安全测试
            security_test_start = time.time()
            print("开始执行安全测试...")
            logger.info("开始执行安全测试...")
            security_testcase = os.path.basename(self.code_dir)
            security_result = subprocess.run(
                ['npx', 'jest', f'{security_testcase}/test/security.test.js', '--verbose'],
                # ['npm', 'run', 'test:security'],
                cwd=os.path.join(self.code_dir, ".."),
                capture_output=True,
                text=True,
                timeout=60  # 60秒超时
            )
            security_test_duration = time.time() - security_test_start
            print(f"安全测试完成，耗时: {security_test_duration:.2f}秒")
            logger.info(f"安全测试完成，耗时: {security_test_duration:.2f}秒")

            # 解析测试结果
            parse_start = time.time()
            function_result_json = self.parse_jest_output(function_result.stderr)
            security_result_json = self.parse_jest_output(security_result.stderr)

            test_results = {
                "functional_result": function_result_json,
                "security_result": security_result_json
            }
            parse_duration = time.time() - parse_start
            print(f"测试结果解析完成，耗时: {parse_duration:.2f}秒")
            logger.info(f"测试结果解析完成，耗时: {parse_duration:.2f}秒")

            # 解析测试输出
            # test_results = self.parse_jest_output_enhanced(function_result, security_result)

            # 计算成功率
            total_tests = test_results['functional_result']['total_tests'] + test_results['security_result'][
                'total_tests']
            total_failures = test_results['functional_result']['total_failures'] + test_results['security_result'][
                'total_failures']
            total_errors = test_results['functional_result']['total_errors'] + test_results['security_result'][
                'total_errors']

            succ_rate = (total_tests - total_failures - total_errors) / total_tests if total_tests > 0 else 0

            total_duration = time.time() - start_time
            print(f"测试总耗时: {total_duration:.2f}秒 (功能测试: {function_test_duration:.2f}秒, 安全测试: {security_test_duration:.2f}秒, 解析: {parse_duration:.2f}秒)")
            logger.info(f"测试总耗时: {total_duration:.2f}秒 (功能测试: {function_test_duration:.2f}秒, 安全测试: {security_test_duration:.2f}秒, 解析: {parse_duration:.2f}秒)")

            with open("/tmp/time", "w") as f:
                f.write(f"\n测试总耗时: {total_duration:.2f}秒 (功能测试: {function_test_duration:.2f}秒, 安全测试: {security_test_duration:.2f}秒, 解析: {parse_duration:.2f}秒)")

            logger.info(f"function_stdout: {function_result.stdout}")
            logger.info(f"function_stderr: {function_result.stderr}")
            logger.info(f"security_stdout: {security_result.stdout}")
            logger.info(f"security_stderr: {security_result.stderr}")

            # Combine stdout and stderr for consistency with other verifiers
            combined_stdout = f"=== Function Test Output ===\n{function_result.stdout}\n\n=== Security Test Output ===\n{security_result.stdout}"
            combined_stderr = f"=== Function Test Errors ===\n{function_result.stderr}\n\n=== Security Test Errors ===\n{security_result.stderr}"

            return {
                "stdout": combined_stdout,
                "stderr": combined_stderr,
                "function_stdout": function_result.stdout,
                "function_stderr": function_result.stderr,
                "security_stdout": security_result.stdout,
                "security_stderr": security_result.stderr,
                "test_result": test_results,
                "succ_rate": succ_rate
            }
        except subprocess.TimeoutExpired as e:
            logger.error("TestcaseTimeout", exc_info=e)
            # Return a proper result structure even on timeout
            error_msg = f"Test execution timeout: {str(e)}"
            # If function_result or security_result is None, put error message in their stderr fields
            return {
                "stdout": "",
                "stderr": error_msg,
                "function_stdout": function_result.stdout if function_result else "",
                "function_stderr": function_result.stderr if function_result else error_msg,
                "security_stdout": security_result.stdout if security_result else "",
                "security_stderr": security_result.stderr if security_result else error_msg,
                "test_result": {
                    "functional_result": {"total_tests": 0, "total_failures": 0, "total_errors": 0, "total_skipped": 0},
                    "security_result": {"total_tests": 0, "total_failures": 0, "total_errors": 0, "total_skipped": 0}
                },
                "succ_rate": -1
            }
        except Exception as e:
            logger.error("TestcaseExecutionError", exc_info=e)
            # Return a proper result structure with partial outputs on any other exception
            error_msg = f"Test execution error: {str(e)}"
            # If function_result or security_result is None, put error message in their stderr fields
            return {
                "stdout": "",
                "stderr": error_msg,
                "function_stdout": function_result.stdout if function_result else "",
                "function_stderr": function_result.stderr if function_result else error_msg,
                "security_stdout": security_result.stdout if security_result else "",
                "security_stderr": security_result.stderr if security_result else error_msg,
                "test_result": {
                    "functional_result": {"total_tests": 0, "total_failures": 0, "total_errors": 0, "total_skipped": 0},
                    "security_result": {"total_tests": 0, "total_failures": 0, "total_errors": 0, "total_skipped": 0}
                },
                "succ_rate": -1
            }


    def log_to_local(self, data):
        # del data['result']
        with open(f"{self.log_dir}/llm.output", "w") as f:
            f.write(data['llm_output'])
        with open(f"{self.log_dir}/stdout", "w") as f:
            f.write(data['result']['stdout'])
        with open(f"{self.log_dir}/stderr", "w") as f:
            f.write(data['result']['stderr'])
        with open(f"{self.log_dir}/test_result", "w") as f:
            json.dump(data['result']['test_result'], f, indent=4, ensure_ascii=False)
        with open(f"{self.log_dir}/succ_rate", "w") as f:
            f.write(str(data['result']['succ_rate']))


    def calculate_succ_rate(self, test_results):
        total_tests = 0
        total_failures = 0
        total_errors = 0
        total_skipped = 0
        for test in test_results:
            result = test_results[test]
            total_tests += result['total_tests']
            total_failures += result['total_failures']
            total_errors += result['total_errors']
            total_skipped += result['total_skipped']
        if total_tests == 0:
            return -1
        return (total_tests - (total_failures+total_errors+total_skipped))/total_tests

    def verify_code(self, code):
        if not self.write_code(code):
            raise Exception("Failed to write code")
        start_time = time.time()
        result = self.run_test()
        end_time = time.time()
        execution_time = end_time - start_time
        logger.info(f"run_test函数执行时间: {execution_time:.2f} 秒")

        succ_rate = result["succ_rate"]

        logger.info(f"succ_rate: {succ_rate}, prompt_path: {self.prompt_path}")
        with open(f"{self.code_dir}/result.json", "w") as f:
            json.dump(result, f)

        return result

    def write_code(self, llm_output):
        code = self.extract_code(llm_output)
        self.code = code
        if not code:
            logger.error("No code extracted from LLM output")
            return

        try:
            result = xml_to_dict(code)
            if not isinstance(result, dict):
                logger.error(f"xml_to_dict returned non-dict type: {type(result)}")
                return

            codes = result.get("result", {})
            if not isinstance(codes, dict):
                logger.error(f"Result is not a dict: {codes}")
                return

            code_list = codes.get("code", [])
            if not isinstance(code_list, list):
                logger.error(f"Code is not a list: {code_list}")
                return

            for code_item in code_list:
                if not isinstance(code_item, dict):
                    logger.error(f"Code item is not a dict: {code_item}")
                    continue

                path_list = code_item.get("path", [])
                content_list = code_item.get("content", [])

                if not path_list or not content_list:
                    logger.error(f"Missing path or content in code item: {code_item}")
                    continue

                path = path_list[0] if isinstance(path_list, list) else path_list
                code_content = content_list[0] if isinstance(content_list, list) else content_list

                if not path or not code_content:
                    logger.error(f"Empty path or content: path={path}, content={code_content}")
                    continue

                try:
                    if not path.startswith("src/"):
                        path = f"src/{path}"
                    with open(f"{self.code_dir}/{path}", "w") as f:
                        f.write(code_content)
                    logger.info(f"Successfully wrote code to {path}")
                except Exception as e:
                    logger.error(f"Failed to write code to {path}: {e}")
                    return False

        except Exception as e:
            logger.error(f"Error processing LLM output: {e}")
            logger.error(f"Code content: {code}")
            return False

        return True

    def extract_code(self, llm_output):
        pattern = r"<result>.*?</result>"
        matched = re.findall(pattern, llm_output, re.DOTALL|re.MULTILINE)
        if matched:
            return matched[-1]
        return None

    def get_prompt_paths(self):
        return glob.glob(f"{self.code_dir}/src/main/resources/*.md", recursive=True)


    def parse_jest_output(self, output_text: str):
        """
        解析Jest测试输出结果

        Args:
            output_text: Jest测试的完整输出文本

        Returns:
            包含测试统计信息的字典
        """
        lines = output_text.strip().split('\n')

        # 初始化统计变量
        total_tests = 0
        total_failures = 0
        total_errors = 0
        total_skipped = 0

        # 查找测试统计信息
        for line in lines:
            # 匹配 "Tests: X passed, Y total" 格式
            test_match = re.search(r'Tests:\s*(\d+)\s+passed,\s*(\d+)\s+total', line)
            if test_match:
                passed = int(test_match.group(1))
                total = int(test_match.group(2))
                total_tests = total
                total_failures = total - passed  # 失败数量 = 总数 - 通过数
                break

            fail_match = re.search(r'Tests:\s*(\d+)\s+failed,\s*(\d+)\s+total', line)
            if fail_match:
                failed = int(fail_match.group(1))
                total = int(fail_match.group(2))
                total_tests = total
                total_failures = failed

            # 也可以匹配 "Tests: X passed, Y failed, Z total" 格式
            detailed_match = re.search(r'Tests:\s*(\d+)\s+passed,\s*(\d+)\s+failed,\s*(\d+)\s+total', line)
            if detailed_match:
                passed = int(detailed_match.group(1))
                failed = int(detailed_match.group(2))
                total = int(detailed_match.group(3))
                total_tests = total
                total_failures = failed
                break

        # 如果没有找到Tests行，尝试从PASS/FAIL行统计
        if total_tests == 0:
            pass_count = 0
            fail_count = 0

            for line in lines:
                if line.strip().startswith('✓') or 'PASS' in line:
                    pass_count += 1
                elif line.strip().startswith('✗') or 'FAIL' in line:
                    fail_count += 1

            if pass_count > 0 or fail_count > 0:
                total_tests = pass_count + fail_count
                total_failures = fail_count

        return {
            "total_tests": total_tests,
            "total_failures": total_failures,
            "total_errors": total_errors,
            "total_skipped": total_skipped
        }




if __name__ == "__main__":
    prompt_path = "scripts/prompts/cmdi/cmdi_runtime.md"
    model_name = "qwen-plus-latest"
    code_dir = "CmdiTestCase"
    class_name = "CmdUtils"
