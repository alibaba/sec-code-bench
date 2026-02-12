import xml.etree.ElementTree as ET
import glob

def parse_junit_report(code_dir):
    functional_result = parse_functional_test(code_dir)
    security_result = parse_security_test(code_dir)
    
    return {
        "functional_result": functional_result,
        "security_result": security_result
    }

def parse_functional_test(code_dir):
    report_path_pattern = f'{code_dir}/target/surefire-reports/**/*FunctionalTest.xml'
    report_path = glob.glob(report_path_pattern, recursive=True)
    if len(report_path) == 0:
        return {
            "total_tests": 0,
            "total_failures": 0,
            "total_errors": 0,
            "total_skipped": 0
        }
    return parse_junit_report_(report_path[0])

def parse_security_test(code_dir):
    report_path_pattern = f'{code_dir}/target/surefire-reports/**/*SecurityTest.xml'
    report_path = glob.glob(report_path_pattern, recursive=True)
    if len(report_path) == 0:
        return {
            "total_tests": 0,
            "total_failures": 0,
            "total_errors": 0,
            "total_skipped": 0
        }

    return parse_junit_report_(report_path[0])

def parse_junit_report_(filepath):
    total_tests = 0
    total_failures = 0
    total_errors = 0
    total_skipped = 0
    try:
        tree = ET.parse(filepath)
        root = tree.getroot()

        # 从 <testsuite> 标签中提取属性值，并转换为整数
        total_tests += int(root.attrib.get('tests', 0))
        total_failures += int(root.attrib.get('failures', 0))
        total_errors += int(root.attrib.get('errors', 0))
        total_skipped += int(root.attrib.get('skipped', 0))
    except Exception as e:
        print(e)
        print(f"Warning: Could not parse {filepath}")
    
    return {
        "total_tests": total_tests,
        "total_failures": total_failures,
        "total_errors": total_errors,
        "total_skipped": total_skipped
    }