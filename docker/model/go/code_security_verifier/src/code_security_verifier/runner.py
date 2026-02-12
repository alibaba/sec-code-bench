import json
import subprocess
import glob
import io
import re
import uuid
import shutil
import os

from code_security_verifier.log_sdk import logger
from code_security_verifier.parse_test_result import parse_junit_report
from code_security_verifier.xml_utils import xml_to_dict
from code_security_verifier.exceptions import InvalidLLMFormatException


class TestCaseRunner:

    def __init__(self, testcase_path, code_dir=None, log_dir=None, request_id=None, prompt_path="default"):
        self._prompt_path = prompt_path
        self.testcase_path = testcase_path
        self.testcase_name = testcase_path.split("/")[-1]
        if not code_dir:
            code_dir = f"/tmp/java_runs/{str(uuid.uuid4())}"
        shutil.copytree(testcase_path, code_dir)
        self.code_dir = code_dir
        if not log_dir:
            self.log_dir = os.path.join(os.path.dirname(__file__), "logs")
        else:
            self.log_dir = log_dir
        self.code = ""
        self._req_id = f'[{request_id}]' if request_id else '[empty]'

    def _detect_language(self, testcase_path):
        """detect the programming language of the testcase based on its files"""
        if os.path.exists(os.path.join(testcase_path, "pom.xml")):
            return "java"
        elif os.path.exists(os.path.join(testcase_path, "src", "go.mod")):
            return "go"
        return "unknown"

    def run_java_test(self):
        try:
            logger.info(f"{self._req_id} code_dir: {self.code_dir}")
            subprocess.run(["mvn", "clean"], cwd=self.code_dir)
            result = subprocess.run(["mvn", "test"], cwd=self.code_dir, capture_output=True, timeout=60, text=True)
            logger.info(f"{self._req_id} stdout: {result.stdout}")
            logger.info(f"{self._req_id} stderr: {result.stderr}")
            return result.stdout, result.stderr
        except subprocess.TimeoutExpired:
            logger.error("{self._req_id} TestcaseTimeout")
            return False, False

    def run_go_test(self):
        # Initialize variables to capture partial results in case of exceptions
        function_stdout = ""
        function_stderr = ""
        security_stdout = ""
        security_stderr = ""

        try:
            src_dir = os.path.join(self.code_dir, 'src')
            logger.info(f"running go mod tidy and goimports commands")
            tidy_result = subprocess.run(["go", "mod", "tidy"], cwd=src_dir, capture_output=True, timeout=30, text=True)
            logger.info(f"{self._req_id} go mod tidy:")
            logger.info(f"{self._req_id} stdout: {tidy_result.stdout}")
            logger.info(f"{self._req_id} stderr: {tidy_result.stderr}")

            for entry in os.listdir(src_dir):
                if not entry.endswith('.go'):
                    continue
                imports_result = subprocess.run(['goimports', '-w', entry], cwd=src_dir, capture_output=True, timeout=30, text=True)
                if imports_result.returncode != 0:
                    logger.error(f'{self._req_id} failed to run goimports command')

            logger.info(f"{self._req_id} code_dir: {self.code_dir}")
            env = os.environ.copy()
            if 'sql' in self.testcase_name:
                logger.info(f"running sql injection related tests, add CGO_ENABLED=1")
                env['CGO_ENABLED'] = '1'

            # 分别运行功能测试和安全测试
            from code_security_verifier.parse_golang_test_result import GO_FUNCTION_TEST_PREFIX, GO_SECURITY_TEST_PREFIX

            # 1. 运行功能测试 (TestFunc*)
            func_result = subprocess.run(
                ["go", "test", "-json", "-run", f"^{GO_FUNCTION_TEST_PREFIX}", "./..."],
                cwd=src_dir, capture_output=True, timeout=60, text=True, env=env
            )
            function_stdout = func_result.stdout
            function_stderr = func_result.stderr
            logger.info(f"{self._req_id} Function test stdout: {function_stdout}")
            logger.info(f"{self._req_id} Function test stderr: {function_stderr}")

            # 2. 运行安全测试 (TestSec*)
            sec_result = subprocess.run(
                ["go", "test", "-json", "-run", f"^{GO_SECURITY_TEST_PREFIX}", "./..."],
                cwd=src_dir, capture_output=True, timeout=60, text=True, env=env
            )
            security_stdout = sec_result.stdout
            security_stderr = sec_result.stderr
            logger.info(f"{self._req_id} Security test stdout: {security_stdout}")
            logger.info(f"{self._req_id} Security test stderr: {security_stderr}")

            from code_security_verifier.parse_golang_test_result import GO_TEST_OUTPUT_FILE

            # 合并所有测试结果到一个文件用于解析
            combined_output = function_stdout + "\n" + security_stdout
            with open(f"{self.code_dir}/{GO_TEST_OUTPUT_FILE}", "w") as f:
                f.write(combined_output)

            return function_stdout, function_stderr, security_stdout, security_stderr
        except subprocess.TimeoutExpired as e:
            logger.error(f"{self._req_id} TestcaseTimeout", exc_info=e)
            # Return partial results with timeout error message
            return (
                function_stdout,
                function_stderr + f"\n[Timeout Error] {str(e)}",
                security_stdout,
                security_stderr
            )
        except Exception as e:
            logger.error(f"{self._req_id} TestcaseExecutionError", exc_info=e)
            # Return partial results with error message
            return (
                function_stdout,
                function_stderr + f"\n[Execution Error] {str(e)}",
                security_stdout,
                security_stderr
            )

    def run_test(self):
        language = self._detect_language(self.testcase_path)
        if language == "java":
            return self.run_java_test()
        elif language == "go":
            return self.run_go_test()
        else:
            logger.error(f"{self._req_id} Unsupported language: {language}")
            return False, False

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


    def create_formatted_code(self) -> str:
        src_dir = os.path.join(self.code_dir, 'src')
        with io.StringIO() as result:
            for file in os.listdir(src_dir):
                if not file.endswith(".go"):
                    continue

                result.write(f'// {file}\n\n')
                with open(os.path.join(src_dir, file), 'r') as f:
                    for line in f:
                        result.write(line)

            content = result.getvalue()
        return content


    def verify_code(self, code):
        function_stdout, function_stderr, security_stdout, security_stderr = "", "", "", ""
        err_msg, test_result = "", {}
        try:
            self.write_code(code)
            test_output = self.run_test()

            lang = self._detect_language(self.testcase_path)
            if lang == "go":
                # Go 返回 4 个值：function_stdout, function_stderr, security_stdout, security_stderr
                if isinstance(test_output, tuple) and len(test_output) == 4:
                    function_stdout, function_stderr, security_stdout, security_stderr = test_output
                from code_security_verifier.parse_golang_test_result import parse_golang_report
                test_result = parse_golang_report(self.code_dir)
            else:
                # Java 返回 2 个值：stdout, stderr
                if isinstance(test_output, tuple) and len(test_output) == 2:
                    stdout, stderr = test_output
                    function_stdout = stdout
                    function_stderr = stderr
                test_result = parse_junit_report(self.code_dir)
            logger.info(f"{self._req_id} test_result: {test_result}")

            succ_rate = self.calculate_succ_rate(test_result)
        except InvalidLLMFormatException as e:
            succ_rate = -1
            err_msg = str(e)
        except Exception as e:
            logger.error(f"{self._req_id} VerifyCodeError", exc_info=e)
            succ_rate = -1
            err_msg = f"Verification error: {str(e)}"

        # 合并输出为统一字段（与其他 verifier 保持一致）
        combined_stdout = f"=== Function Test Output ===\n{function_stdout}\n\n=== Security Test Output ===\n{security_stdout}"
        combined_stderr = f"=== Function Test Errors ===\n{function_stderr}\n\n=== Security Test Errors ===\n{security_stderr}"

        result = {
            "stdout": combined_stdout.strip(),
            "stderr": combined_stderr.strip(),
            "function_stdout": function_stdout,
            "function_stderr": function_stderr,
            "security_stdout": security_stdout,
            "security_stderr": security_stderr,
            "test_result": test_result,
            "succ_rate": succ_rate,
            "err_msg": err_msg,
        }

        if succ_rate == 1.0:
            formatted_code = self.create_formatted_code()
            logger.info({"llm_output": code, "result": result, "succ_rate": succ_rate, "prompt_path": self._prompt_path, "formatted_code": formatted_code})
        else:
            logger.info(f"{self._req_id} code: {code}, result: {result}, succ_rate: {succ_rate}")
        with open(f"{self.code_dir}/result.json", "w") as f:
            json.dump(result, f)

        return result

    def write_code(self, llm_output):
        code = self.extract_code(llm_output)
        self.code = code
        if not code:
            logger.error(f"{self._req_id} No code extracted from LLM output")
            raise InvalidLLMFormatException("No code extracted from LLM output")

        # Try to parse as JSON first (for Go tasks)
        result = None
        if code.strip().startswith('{'):
            try:
                result = json.loads(code)
            except json.JSONDecodeError as je:
                logger.warning(f"{self._req_id} Failed to parse as JSON: {je}")

        # If JSON parsing failed, try XML parsing (for Java tasks)
        if result is None:
            try:
                result = xml_to_dict(code)
            except Exception as xe:
                logger.error(f"{self._req_id} Failed to parse as XML: {xe}")
                raise InvalidLLMFormatException("Invalid format when extracting code from LLM output")

        if not isinstance(result, dict):
            logger.error(f"{self._req_id} Parsing returned non-dict type: {type(result)}")
            raise InvalidLLMFormatException(f'Parsing returned non-dict type: {type(result)}')

        codes = result.get("result", {})
        if not isinstance(codes, dict) or not codes:
            logger.error(f"{self._req_id} Empty result key")
            raise InvalidLLMFormatException('Empty code')

        code_list = codes.get("code", [])
        if not isinstance(code_list, list) or not code_list:
            logger.error(f"{self._req_id} code list")
            raise InvalidLLMFormatException('Empty code list')

        for code_item in code_list:
            if not isinstance(code_item, dict):
                logger.error(f"{self._req_id} Code item is not a dict: {code_item}")
                continue

            path_list = code_item.get("path", [])
            content_list = code_item.get("content", [])

            if not path_list or not content_list:
                logger.error(f"{self._req_id} Missing path or content in code item: {code_item}")
                continue

            path = path_list[0] if isinstance(path_list, list) else path_list
            code_content = content_list[0] if isinstance(content_list, list) else content_list

            if not path or not code_content:
                logger.error(f"{self._req_id} Empty path or content: path={path}, content={code_content}")
                continue

            language = self._detect_language(self.testcase_path)
            if not path.startswith("src/main/java") and language == "java":
                path = f"src/main/java/{path}"
            elif language == "go":
                if path == "go.mod":
                    continue
                if not path.endswith(".go"):
                    path = "main.go"
            source_filename = f'{self.code_dir}/src/{path}'
            os.makedirs(os.path.dirname(source_filename), exist_ok=True)
            with open(f"{self.code_dir}/src/{path}", "w") as f:
                f.write(code_content)
            logger.info(f"{self._req_id} Successfully wrote code to {path}")


    def extract_code(self, llm_output):
        # Try to find JSON format first
        # Look for a JSON object that contains "result" field
        # Simple approach: find the first { and last } that form a valid JSON with "result" field
        try:
            json.loads(llm_output)
            return llm_output
        except json.JSONDecodeError:
            pass

        start_idx = llm_output.rfind('```json')
        end_idx = llm_output.rfind('```')
        if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
            json_str = llm_output[start_idx + len('```json'):end_idx].strip()
            try:
                json_obj = json.loads(json_str)
                if "result" in json_obj:
                    return json_str
            except json.JSONDecodeError:
                pass

        # If no JSON found, try XML format
        xml_pattern = r"<result>.*?</result>"
        xml_matched = re.findall(xml_pattern, llm_output, re.DOTALL|re.MULTILINE)
        if xml_matched:
            return xml_matched[-1]

        return None

    def get_prompt_paths(self):
        return glob.glob(f"{self.code_dir}/src/main/resources/*.md", recursive=True)


if __name__ == "__main__":
    prompt_path = "scripts/prompts/cmdi/cmdi_runtime.md"
    model_name = "qwen-plus-latest"
    code_dir = "CmdiTestCase"
    class_name = "CmdUtils"
