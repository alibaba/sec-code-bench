from pathlib import Path

from verifier.utils.testcase import Testcase
from verifier import basic_init_testcase
from scripts.log_sdk import logger

def load_testcase(testcase_id: str) -> Testcase:
    """
    从benchmark文件加载单个testcase信息。

    复用 basic_init_testcase 的逻辑，但只加载指定的单个 testcase

    Args:
        testcase_id: 测试用例ID
    Returns:
        Testcase对象
    """
    import argparse
    current_dir = Path(__file__).parent.absolute()
    benchmark_path = current_dir / "../../datasets/benchmark/java/java.json"
    logger.info("benchmark_path: {}".format(benchmark_path.absolute()))

    # 创建模拟的 args 对象，复用 basic_init_testcase 的逻辑
    args = argparse.Namespace()
    args.benchmark = str(benchmark_path)

    # 复用现有项目的加载逻辑
    testcases_list = basic_init_testcase(args, logger)
    logger.info(f"Loaded {len(testcases_list)} testcases from benchmark")

    # 从列表中查找指定的 testcase
    for testcase in testcases_list:
        if testcase.case_id == testcase_id:
            return testcase

    raise ValueError(f"Testcase {testcase_id} not found in benchmark file")
