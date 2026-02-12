import os
import shutil
import uuid
import asyncio
import json
from pathlib import Path

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from scripts.log_sdk import logger

# 导入评测相关模块
from verifier.evaluator.base import EvaluatorResult
from verifier.llm.llm_manager import LLMManager
from verifier.tester.function import FunctionTester
from verifier.tester.security import SecurityTester
from verifier.utils.testcase import Testcase
from verifier.utils.testcase_util import load_testcase
from verifier.utils.fdisk_utils import save_file
from verifier.eval import format_response
from verifier import basic_init_llm


PYTHON_PATH = os.environ.get("PYTHON_PATH", "python3")
# --- FastAPI 应用 ---
app = FastAPI()


# Note: XML parsing functions moved to verify_xml.py for subprocess architecture


# 定义请求体的模型
# judge_llm_list format: ["PROVIDER::MODEL::API_KEY::ENDPOINT", ...]
class CodePayload(BaseModel):
    code: str
    token: str
    prompt_path: str = "default"
    judge_llm_list: list[str] | None = None
    locale: str = "en-US"


def init_llm_manager(judge_llm_list: list[str]) -> LLMManager:
    """
    初始化LLMManager。

    复用 basic_init_llm 的逻辑，通过传入固定的 judge_llm_list 列表

    Args:
        judge_llm_list: 固定的 judge LLM 列表，格式为 ["PROVIDER::MODEL::API_KEY::BASE_URL", ...]
                        如果为 None，则从环境变量 JUDGE_LLM_LIST 读取（向后兼容）

    Returns:
        LLMManager 对象
    """
    import argparse

    # 创建模拟的 args 对象，复用 basic_init_llm 的逻辑
    args = argparse.Namespace()

    args.judge_llm_list = judge_llm_list

    # 复用现有项目的初始化逻辑
    return basic_init_llm(args, logger)


def _run_function_eval_in_thread(
    testcase: Testcase,
    code_dir: Path,
    llm_manager: LLMManager,
    judge_list: list[str],
    locale: str,
) -> EvaluatorResult:
    """
    在线程中运行功能测试，避免事件循环冲突。
    这个函数会在完全独立的线程中运行，确保 asyncio.run() 可以正常工作。
    """
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
                locale=locale,
            )
            result_queue.put(result)
        except Exception as e:
            exception_queue.put(e)

    thread = threading.Thread(target=run_in_thread, daemon=False)
    thread.start()
    thread.join(timeout=600)  # 10分钟超时

    if not exception_queue.empty():
        raise exception_queue.get()

    if not result_queue.empty():
        return result_queue.get()

    raise TimeoutError("Function evaluation timeout")


def _run_security_eval_in_thread(
    testcase: Testcase,
    code_dir: Path,
    llm_manager: LLMManager,
    judge_list: list[str],
    locale: str,
) -> EvaluatorResult:
    """
    在线程中运行安全测试，避免事件循环冲突。
    这个函数会在完全独立的线程中运行，确保 asyncio.run() 可以正常工作。
    """
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
                locale=locale,
            )
            result_queue.put(result)
        except Exception as e:
            exception_queue.put(e)

    thread = threading.Thread(target=run_in_thread, daemon=False)
    thread.start()
    thread.join(timeout=600)  # 10分钟超时

    if not exception_queue.empty():
        raise exception_queue.get()

    if not result_queue.empty():
        return result_queue.get()

    raise TimeoutError("Security evaluation timeout")


@app.post("/verify/{testcase}")
async def verify_by_xml_api(testcase: str, payload: CodePayload):
    """
    使用已生成的XML格式代码进行评分 - 基于 subprocess 架构

    与 C-Verifier 保持一致的架构：
    - 通过 subprocess 调用独立的 verify_xml.py 脚本
    - Python 解释器自动处理转义字符
    - 结果通过 JSON 文件返回

    Args:
        testcase: 测试用例ID
        payload: 包含XML格式代码的请求体

    Returns:
        评分结果，包含功能测试和安全测试的结果
    """
    # 验证token
    if payload.token != "local-eval-token":
        raise HTTPException(status_code=401, detail="Invalid token")

    # 验证testcase名称
    if ".." in testcase or "/" in testcase or "\\" in testcase:
        raise HTTPException(status_code=400, detail="Invalid testcase name.")

    # 创建临时工作目录
    run_id = str(uuid.uuid4())
    work_dir = Path(f"/tmp/java_runs/{run_id}")
    work_dir.mkdir(parents=True, exist_ok=True)

    # 初始化返回结果
    result = {
        "stdout": "",
        "stderr": "",
        "test_result": {
            "functional_result": {
                "total_tests": 0,
                "total_failures": 0,
                "total_errors": 0,
                "total_skipped": 0,
            },
            "security_result": {
                "total_tests": 0,
                "total_failures": 0,
                "total_errors": 0,
                "total_skipped": 0,
            },
        },
        "succ_rate": -1,
    }

    try:
        # 准备参数（与 C-Verifier 一致的方式）
        prompt_path = payload.prompt_path if payload.prompt_path else "default"
        judge_llm_list_json = (
            json.dumps(payload.judge_llm_list) if payload.judge_llm_list else "[]"
        )
        locale = (
            payload.locale if hasattr(payload, "locale") and payload.locale else "en-US"
        )

        logger.info(f"[DEBUG] Starting verify_xml for testcase: {testcase}")
        logger.info(f"[DEBUG] Work dir: {work_dir}")
        logger.info(f"[DEBUG] Prompt path: {prompt_path}")
        logger.info(f"[DEBUG] Code length: {len(payload.code)}")

        # 调用独立的 verify_xml.py 脚本（与 C-Verifier 完全一致的架构）
        logger.info(f"[DEBUG] Creating subprocess...")
        process = await asyncio.create_subprocess_exec(
            PYTHON_PATH,
            "verify_xml.py",
            testcase,
            str(work_dir),
            payload.code,  # Python 解释器会自动处理转义字符
            prompt_path,
            judge_llm_list_json,
            locale,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
        )
        logger.info(f"[DEBUG] Subprocess created, PID: {process.pid}")

        # 等待命令执行完成
        logger.info(f"[DEBUG] Waiting for process.communicate()...")
        stdout, stderr = await process.communicate()
        logger.info(
            f"[DEBUG] process.communicate() completed, returncode: {process.returncode}"
        )

        if process.returncode == 0:
            logger.info("verify_xml.py completed successfully")
        else:
            logger.error(f"verify_xml.py failed with return code {process.returncode}")
            logger.error(f"stderr: {stderr.decode()}")

        # Log subprocess output for debugging
        if stdout:
            logger.info(f"verify_xml.py stdout:\n{stdout.decode()}")
        if stderr:
            logger.info(f"verify_xml.py stderr:\n{stderr.decode()}")

        # 读取结果文件（与 C-Verifier 一致）
        logger.info(f"[DEBUG] Reading result file...")
        result_file = work_dir / "result.json"
        if result_file.exists():
            logger.info(f"[DEBUG] Result file exists, size: {result_file.stat().st_size}")
            with open(result_file) as f:
                result = json.load(f)
            logger.info(f"[DEBUG] Result loaded successfully")
        else:
            logger.error("Result file not found")
            result["stderr"] = "Result file not found"

    except Exception as e:
        logger.error(f"Error in verify_by_xml_api: {e}", exc_info=True)
        result["stderr"] = str(e)
        result["succ_rate"] = 0.0

    finally:
        # 清理临时目录
        logger.info(f"[DEBUG] Entering finally block...")
        if work_dir.exists():
            try:
                logger.info(f"[DEBUG] Cleaning up work directory: {work_dir}")
                shutil.rmtree(work_dir)
                logger.info(f"[DEBUG] Work directory cleaned successfully")
            except Exception as e:
                logger.warning(f"Failed to cleanup work directory: {e}")

    # 使用 JSONResponse 确保返回正确的 JSON 格式
    logger.info(f"[DEBUG] Preparing JSONResponse...")
    logger.info(f"[DEBUG] Result succ_rate: {result['succ_rate']}")
    response = JSONResponse(content=result, media_type="application/json")
    logger.info(f"[DEBUG] Returning JSONResponse")
    return response



@app.get("/")
def read_root():
    return {"message": "Java Verifier is ready"}


@app.get("/health")
def health_check():
    return {"status": "healthy"}
