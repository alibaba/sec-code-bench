import json
import subprocess
import glob
import re
import uuid
import shutil
import os

from code_security_verifier.log_sdk import logger
from code_security_verifier.parse_test_result import parse_junit_report
from code_security_verifier.xml_utils import xml_to_dict



class TestCaseRunner:
    
    def __init__(self, testcase_path, code_dir=None, log_dir=None):
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

    
    def run_test(self):
        try:
            logger.info(f"code_dir: {self.code_dir}")
            subprocess.run(["mvn", "clean"], cwd=self.code_dir)
            result = subprocess.run(["mvn", "test"], cwd=self.code_dir, capture_output=True, timeout=60, text=True)
            logger.info(f"stdout: {result.stdout}")
            logger.info(f"stderr: {result.stderr}")
            return result.stdout, result.stderr
        except subprocess.TimeoutExpired:
            logger.error("TestcaseTimeout")
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
    
    def verify_code(self, code):
        if not self.write_code(code):
            raise Exception("Failed to write code")
        
        stdout, stderr = self.run_test()
        test_result = parse_junit_report(self.code_dir)
        logger.info(f"test_result: {test_result}")
        
        succ_rate = self.calculate_succ_rate(test_result)
        
        result = {
            "stdout": stdout, 
            "stderr": stderr, 
            "test_result": test_result,
            "succ_rate": succ_rate,
        }

        if succ_rate == 1.0:
            logger.info({"llm_output": code, "result": result, "succ_rate": succ_rate})
        else:
            logger.info(f"code: {code}, result: {result}, succ_rate: {succ_rate}")
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
                    if not path.startswith("src/main/java"):
                        path = f"src/main/java/{path}"
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
        logger.info(f"LLM output: {llm_output}")
        pattern = r"<result>.*?</result>"
        matched = re.findall(pattern, llm_output, re.DOTALL|re.MULTILINE)
        if matched:
            return matched[-1]
        return None
    
    def get_prompt_paths(self):
        return glob.glob(f"{self.code_dir}/src/main/resources/*.md", recursive=True)
    
        
class CTestCaseRunner(TestCaseRunner):

    def __init__(self, testcase_path, code_dir, log_dir=None):
        self.testcase_path = testcase_path
        self.testcase_name = testcase_path.split("/")[-1]
        if not code_dir:
            code_dir = f"/tmp/c_runs/{str(uuid.uuid4())}"
        shutil.copytree(testcase_path, code_dir)
        self.code_dir = code_dir
        if not log_dir:
            self.log_dir = os.path.join(os.path.dirname(__file__), "logs")
        else:
            self.log_dir = log_dir
        self.code = ""
    def run_test(self):
        try:
            logger.info(f"code_dir: {self.code_dir}")
            tests_dir = os.path.join(self.code_dir, "tests")

            # 1. 构建测试
            logger.info("Building tests...")
            subprocess.run(
                ["rm", "-rf", "build"],
                cwd=tests_dir,
                check=False
            )
            subprocess.run(
                ["mkdir", "-p", "build"],
                cwd=tests_dir,
                check=True
            )
            subprocess.run(
                ["cmake", ".."],
                cwd=os.path.join(tests_dir, "build"),
                check=True,
                capture_output=True,
                timeout=30
            )
            subprocess.run(
                ["make"],
                cwd=os.path.join(tests_dir, "build"),
                check=True,
                capture_output=True,
                timeout=30
            )

            # 2. 分别运行功能测试和安全测试
            logger.info("Running function test...")
            func_result = subprocess.run(
                ["ctest", "--timeout", "10", "--verbose", "-R", "^FunctionTest$"],
                cwd=os.path.join(tests_dir, "build"),
                capture_output=True,
                timeout=15,
                text=True
            )
            function_stdout = func_result.stdout
            function_stderr = func_result.stderr
            func_returncode = func_result.returncode
            logger.info(f"Function test stdout: {function_stdout}")
            logger.info(f"Function test stderr: {function_stderr}")

            logger.info("Running security test...")
            sec_result = subprocess.run(
                ["ctest", "--timeout", "10", "--verbose", "-R", "^SecurityTest$"],
                cwd=os.path.join(tests_dir, "build"),
                capture_output=True,
                timeout=15,
                text=True
            )
            security_stdout = sec_result.stdout
            security_stderr = sec_result.stderr
            sec_returncode = sec_result.returncode
            logger.info(f"Security test stdout: {security_stdout}")
            logger.info(f"Security test stderr: {security_stderr}")

            # 返回码：如果任一测试失败，则返回非零
            return_code = func_returncode or sec_returncode

            return function_stdout, function_stderr, security_stdout, security_stderr, return_code
        except subprocess.TimeoutExpired:
            logger.error("TestcaseTimeout")
            return False, False, False, False, -1
        except Exception as e:
            logger.error(f"Test execution error: {e}")
            return False, False, False, False, -1
    def generate_ctest_result(self, function_stdout, security_stdout):
        """根据分离的测试输出生成结果"""
        func_test_passed = "FunctionTest .....................   Passed" in function_stdout
        sec_test_passed = "SecurityTest .....................   Passed" in security_stdout

        total_func_failure = 0 if func_test_passed else 1
        total_sec_failure = 0 if sec_test_passed else 1

        func_test_res = {
            "total_tests": 1,
            "total_failures": total_func_failure,
            "total_errors": 0,
            "total_skipped": 0
        }
        sec_test_res = {
            "total_tests": 1,
            "total_failures": total_sec_failure,
            "total_errors": 0,
            "total_skipped": 0
        }
        succ_rate = 1.0 - ((total_func_failure + total_sec_failure) / 2)
        return func_test_res, sec_test_res, succ_rate

    def verify_code(self, code):
        if not self.write_code(code):
            raise Exception("Failed to write code")

        function_stdout, function_stderr, security_stdout, security_stderr, return_code = self.run_test()
        func_test_res, sec_test_res, succ_rate = self.generate_ctest_result(function_stdout, security_stdout)

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
            "returncode": return_code,
            "test_result": {"functional_result": func_test_res, "security_result": sec_test_res},
            "succ_rate": succ_rate,
        }

        if succ_rate == 1.0:
            logger.info({"llm_output": code, "result": result, "succ_rate": succ_rate})
        else:
            logger.info(f"code: {code}, result: {result}, succ_rate: {succ_rate}")
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

if __name__ == "__main__":
    prompt_path = "scripts/prompts/cmdi/cmdi_runtime.md"
    model_name = "qwen-plus-latest"
    code_dir = "CmdiTestCase"
    class_name = "CmdUtils"