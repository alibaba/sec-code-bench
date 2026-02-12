import sys
from code_security_verifier.runner import TestCaseRunner

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python main.py <testcase_name>")
        sys.exit(1)

    testcase_name = sys.argv[1]
    prompt_path = sys.argv[2]
    code_dir = sys.argv[3]
    code = sys.argv[4]

    runner = TestCaseRunner(f"testcases/{testcase_name}", prompt_path, code_dir)
    runner.verify_code(code)
