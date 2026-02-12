import sys
from code_security_verifier.runner import TestCaseRunner


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python main.py <testcase_name>")
        sys.exit(1)

    testcase_name = sys.argv[1]
    code_dir = sys.argv[2]
    code = sys.argv[3]
    request_id = sys.argv[4]
    prompt_path = sys.argv[5]
    runner = TestCaseRunner(f"bench/req-gen-verified/{testcase_name}", code_dir=code_dir, request_id=request_id, prompt_path=prompt_path)
    runner.verify_code(code)
