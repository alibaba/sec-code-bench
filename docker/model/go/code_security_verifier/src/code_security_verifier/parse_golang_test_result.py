import os
import json

GO_TEST_OUTPUT_FILE = "go_test_output.json"
GO_FUNCTION_TEST_PREFIX = "TestFunc"
GO_SECURITY_TEST_PREFIX = "TestSec"

def parse_golang_report(code_dir):
    report_path = os.path.join(code_dir, GO_TEST_OUTPUT_FILE)
    total_tests = 0
    total_failures = 0
    total_skipped = 0

    ret = {
        "functional_result": {
            "total_tests": total_tests,
            "total_failures": total_failures,
            "total_errors": 0,
            "total_skipped": total_skipped
        },
        "security_result": {
            "total_tests": total_tests,
            "total_failures": total_failures,
            "total_errors": 0,
            "total_skipped": total_skipped
        }
    }

    if not os.path.exists(report_path):
        return ret

    test_cases = set()
    succeded_tests = set()
    skiped_tests = set()
    with open(report_path, "r") as f:
        for line in f:
            try:
                test_event = json.loads(line)
                testName = test_event.get("Test")
                if testName:
                    test_cases.add(testName)

                action = test_event.get("Action")
                if action == "pass" and testName:
                    succeded_tests.add(testName)
                elif action == "skip" and testName:
                    skiped_tests.add(testName)

                # there is not "fail" action in go test json output, failures are indicated by absence of "pass" or "skip"
            except json.JSONDecodeError:
                continue

    ret["functional_result"]["total_tests"] = sum(1 for name in test_cases if name.startswith(GO_FUNCTION_TEST_PREFIX))
    ret["security_result"]["total_tests"] = sum(1 for name in test_cases if name.startswith(GO_SECURITY_TEST_PREFIX))
    ret["functional_result"]["total_skipped"] = sum(1 for name in skiped_tests if name.startswith(GO_FUNCTION_TEST_PREFIX))
    ret["security_result"]["total_skipped"] = sum(1 for name in skiped_tests if name.startswith(GO_SECURITY_TEST_PREFIX))
    ret["functional_result"]["total_failures"] = ret["functional_result"]["total_tests"] - sum(1 for name in (succeded_tests | skiped_tests) if name.startswith(GO_FUNCTION_TEST_PREFIX))
    ret["security_result"]["total_failures"] = ret["security_result"]["total_tests"] - sum(1 for name in (succeded_tests | skiped_tests) if name.startswith(GO_SECURITY_TEST_PREFIX))

    return ret

if __name__ == "__main__":
    # for test only
    code_dir = "/data1/user-data/ruogui.ygr/Code/codescanner/CodeSecurityTestCase/testcases/GolangZipBomb/src"
    result = parse_golang_report(code_dir)
    print(result)