import os
import shutil
import uuid
from pathlib import Path
import asyncio
import json

from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
from code_security_verifier.log_sdk import logger

PYTHON_PATH = os.environ.get("PYTHON_PATH", "python3")
# --- FastAPI 应用 ---
app = FastAPI()

def get_client_ip(request: Request) -> str:
    """
    获取客户端真实IP地址
    优先从 X-Forwarded-For 或 X-Real-IP 头获取（适用于反向代理场景）
    否则使用 request.client.host
    """
    # 检查 X-Forwarded-For 头（可能包含多个IP，取第一个）
    forwarded_for = request.headers.get("X-Forwarded-For")
    if forwarded_for:
        # X-Forwarded-For 可能包含多个IP，用逗号分隔，取第一个
        return forwarded_for.split(",")[0].strip()
    
    # 检查 X-Real-IP 头
    real_ip = request.headers.get("X-Real-IP")
    if real_ip:
        return real_ip.strip()
    
    # 直接连接的客户端IP
    if request.client:
        return request.client.host
    
    return "unknown"

# 定义请求体的模型
class CodePayload(BaseModel):
    prompt_path: str
    code: str
    token: str

@app.post("/verify/{testcase}")
async def verify(testcase: str, payload: CodePayload, request: Request):
    token = payload.token
    if token != "local-eval-token":
        raise HTTPException(status_code=401, detail="Invalid token")
    
    # 验证传入的类名是否合法，防止路径遍历攻击
    if ".." in testcase or "/" in testcase or "\\" in testcase:
        raise HTTPException(status_code=400, detail="Invalid testcase name.")

    # 1. 为每个请求创建一个唯一的临时工作目录
    # 使用 uuid 保证目录名唯一，避免并发问题
    run_id = str(uuid.uuid4())
    Path("/tmp/c_runs").mkdir(exist_ok=True)
    temp_dir_path = Path(f"/tmp/c_runs/{run_id}") # 在Linux/macOS上建议使用/tmp


    # logger.info(f"code: {payload.code}")
    stdout = b""
    stderr = b""
    
    result = {
        "stdout": stdout.decode(),
        "stderr": stderr.decode(),
        "returncode": -1,
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
            temp_dir_path,
            payload.code,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE
        )
        
        # 等待命令执行完成并获取输出
        
        stdout, stderr = await process.communicate()
        if process.returncode == 0:
            print("Success:", stdout.decode())
        else:
            print(f"Error: command failed with return code {process.returncode}")
            print("stderr:", stderr.decode())

        with open(f"{temp_dir_path}/result.json") as f:
            result = json.load(f)
        shutil.rmtree(temp_dir_path)
    except Exception as e:
        logger.error("verifyCodeError", exc_info=e)

    return result

        

@app.get("/")
def read_root():
    return {"message": "Java Test Runner is ready. Please POST to /run-test/{test_class_name}"}