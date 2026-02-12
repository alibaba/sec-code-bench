import os
import shutil
import uuid
from pathlib import Path
import asyncio
import json
import os
import time
import threading
from typing import Dict, List, Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from code_security_verifier.log_sdk import logger


PYTHON_PATH = os.environ.get("PYTHON_PATH", "python3")
# --- FastAPI 应用 ---
app = FastAPI()

# 定义请求体的模型
class CodePayload(BaseModel):
    code: str
    token: str
    prompt_path: str = "default"

@app.post("/verify/{testcase}")
async def verify(testcase: str, payload: CodePayload):
    token = payload.token
    logger.info(f"testcase: {testcase}")
    logger.info(f"payload: {payload.prompt_path}")
    if token != "local-eval-token":
        raise HTTPException(status_code=401, detail="Invalid token")
    
    # 验证传入的类名是否合法，防止路径遍历攻击
    if ".." in testcase or "/" in testcase or "\\" in testcase:
        raise HTTPException(status_code=400, detail="Invalid testcase name.")

    # 1. 为每个请求创建一个唯一的临时工作目录
    # 使用 uuid 保证目录名唯一，避免并发问题
    run_id = str(uuid.uuid4())
    temp_dir_path = Path(f"/tmp/java_runs/{run_id}") # 在Linux/macOS上建议使用/tmp

    logger.info(f"code: {payload.code} prompt_path:{payload.prompt_path}")
    stdout = b""
    stderr = b""
    
    result = {
        "stdout": stdout.decode(),
        "stderr": stderr.decode(),
        # 格式错误
        "test_result": {},
        "succ_rate": -1,
    }
    try:
        # 2. 复制Maven模板项目到临时目录
        process = await asyncio.create_subprocess_exec(
            PYTHON_PATH, 
            "code_security_verifier/src/code_security_verifier/verify.py",
            testcase,
            payload.prompt_path,
            temp_dir_path,
            payload.code,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE
        )
        
        # 等待命令执行完成并获取输出
        stdout, stderr = await process.communicate()
        # print(stdout)
        # print(stderr)
        # print("dddddd")
        with open(f"{temp_dir_path}/result.json") as f:
            result = json.load(f)
        shutil.rmtree(temp_dir_path)
    except Exception as e:
        logger.error("verifyCodeError", exc_info=e)
    
    logger.info(f"verify_result: {result} {payload.prompt_path}")
    return result

        

@app.get("/")
def read_root():
    return {"message": "Java Test Runner is ready. Please POST to /run-test/{test_class_name}"}


log_records: Dict[str, List[tuple]] = {}
lock = threading.Lock()

# 清理过期记录的阈值（30秒）
CLEANUP_THRESHOLD = 60

class LogResponse(BaseModel):
    path: str
    timestamp: float
    message: str

class CheckResponse(BaseModel):
    path: str
    records: List[dict]
    total_count: int

def cleanup_expired_records():
    """清理过期的记录（超过30秒的记录）"""
    current_time = time.time()
    with lock:
        expired_paths = []
        for path, records in log_records.items():
            # 过滤掉过期的记录
            valid_records = [(ts, count) for ts, count in records if current_time - ts <= CLEANUP_THRESHOLD]
            if valid_records:
                log_records[path] = valid_records
            else:
                expired_paths.append(path)
        
        # 删除没有有效记录的路径
        for path in expired_paths:
            del log_records[path]

def add_log_record(path: str):
    """添加日志记录"""
    current_time = time.time()
    with lock:
        if path not in log_records:
            log_records[path] = []
        
        # 检查是否已有相同时间戳的记录
        if log_records[path] and log_records[path][-1][0] == current_time:
            # 如果时间戳相同，增加计数
            last_timestamp, last_count = log_records[path][-1]
            log_records[path][-1] = (last_timestamp, last_count + 1)
        else:
            # 添加新记录
            log_records[path].append((current_time, 1))

@app.api_route("/log/{path:path}", methods=["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"])
async def log_path(path: str):
    """记录请求路径，支持所有HTTP方法"""
    add_log_record(path)
    cleanup_expired_records()
    
    return LogResponse(
        path=path,
        timestamp=time.time(),
        message=f"路径 '{path}' 已记录"
    )

@app.get("/check")
async def check_path(path: Optional[str] = None):
    """根据路径获取请求记录"""
    cleanup_expired_records()
    
    with lock:
        if path not in log_records:
            raise HTTPException(status_code=404, detail=f"路径 '{path}' 没有找到记录")
        
        records = log_records[path]
        formatted_records = [
            {
                "timestamp": ts,
                "count": count,
                "time_ago": time.time() - ts
            }
            for ts, count in records
        ]
        
        total_count = sum(count for _, count in records)
        
        return CheckResponse(
            path=path,
            records=formatted_records,
            total_count=total_count
        )