import os
import shutil
import uuid
from pathlib import Path
import asyncio
import json
import os

from fastapi import FastAPI, HTTPException, Response
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
async def verify(response: Response, testcase: str, payload: CodePayload):
    request_id = str(uuid.uuid4())
    response.headers["X-Request-ID"] = request_id
    token = payload.token
    if token != "local-eval-token":
        raise HTTPException(status_code=401, detail="Invalid token")
    
    # 验证传入的类名是否合法，防止路径遍历攻击
    if ".." in testcase or "/" in testcase or "\\" in testcase:
        raise HTTPException(status_code=400, detail="Invalid testcase name")

    # 1. 为每个请求创建一个唯一的临时工作目录
    # 使用 uuid 保证目录名唯一，避免并发问题
    run_id = str(uuid.uuid4())
    temp_dir_path = Path(f"/tmp/java_runs/{run_id}") # 在Linux/macOS上建议使用/tmp

    logger.info(f"[{request_id}] code: {payload.code}")
    
    try:
        # 2. 复制Maven模板项目到临时目录
        process = await asyncio.create_subprocess_exec(
            PYTHON_PATH, 
            "code_security_verifier/src/code_security_verifier/verify.py",
            testcase,
            temp_dir_path,
            payload.code,
            request_id,
            payload.prompt_path,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
            cwd=os.getcwd(),
        )
        
        # 等待命令执行完成并获取输出
        stdout, stderr = await process.communicate()
        logger.info(f"[{request_id}] stdout: {stdout.decode('utf-8')}")
        logger.info(f"[{request_id}] stderr: {stderr.decode('utf-8')}")
        with open(f"{temp_dir_path}/result.json") as f:
            result = json.load(f)
        shutil.rmtree(temp_dir_path)
        logger.info(f"[{request_id}] verify_result: {result}")
        return result
    except Exception as e:
        logger.error(f"[{request_id}] verifyCodeError", exc_info=e)
        raise HTTPException(status_code=500, detail="Internal server error, please send the request ID to developers")
    

        

@app.get("/")
def read_root():
    return {"message": "Java Test Runner is ready. Please POST to /run-test/{test_class_name}"}
