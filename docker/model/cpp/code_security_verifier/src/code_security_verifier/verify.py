import sys
from code_security_verifier.runner import TestCaseRunner, CTestCaseRunner

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python main.py <testcase_name>")
        sys.exit(1)
    # logger.info("verify.py")
    is_C_prj = True
    testcase_name = sys.argv[1]
    code_dir = sys.argv[2]
    code = sys.argv[3]
    if is_C_prj:
        runner = CTestCaseRunner(f"testcases/{testcase_name}", code_dir)
    else:
        runner = TestCaseRunner(f"testcases/{testcase_name}", code_dir)
    runner.verify_code(code)