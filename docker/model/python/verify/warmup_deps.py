import os
import subprocess
import sys
import shutil
import hashlib
import re

# 假设运行在项目根目录
OUTPUT_REPOS_DIR = os.path.join(os.getcwd(), 'python_bench')


def process_requirements_for_linux(req_path):
    """
    Check if requirements.txt contains torch.
    If so, create a temp file with torch==x.x.x changed to torch==x.x.x+cpu
    to avoid downloading huge GPU wheels.
    """
    if sys.platform != "linux":
        return req_path

    try:
        with open(req_path, 'r', encoding='utf-8') as f:
            content = f.read()

        # Simple check first
        if 'torch==' not in content:
            return req_path

        # Regex replacement: torch==2.5.0 -> torch==2.5.0+cpu
        # Be careful not to double replace or replace if already has suffix
        pattern = r"torch==(\d+\.\d+\.\d+)(?!\+)"
        new_content, count = re.subn(pattern, r"torch==\1+cpu", content)

        if count == 0:
            return req_path

        # Create temp file
        temp_req_path = req_path + ".cpu.tmp"
        with open(temp_req_path, 'w', encoding='utf-8') as f:
            f.write(new_content)

        return temp_req_path

    except Exception as e:
        print(f"Warning: Failed to process requirements file {req_path}: {e}")
        return req_path


def main():
    print("--- Starting Dependency Warmup (Enhanced) ---")
    if not os.path.exists(OUTPUT_REPOS_DIR):
        print(f"Directory not found: {OUTPUT_REPOS_DIR}")
        return

    # 1. 检查 uv
    uv_path = shutil.which("uv")
    if not uv_path:
        print("Error: 'uv' not found. Please install uv first.")
        sys.exit(1)

    # 2. 定义临时 venv 路径
    temp_venv = os.path.join(os.getcwd(), 'temp_warmup_venv')

    # 定义 python 路径变量 (路径字符串是固定的，但文件会在循环中重建)
    if sys.platform == "win32":
        python_executable = os.path.join(temp_venv, 'Scripts', 'python')
    else:
        python_executable = os.path.join(temp_venv, 'bin', 'python')

    # 3. 收集所有 requirements.txt 并去重
    print("Scanning for requirements.txt files...")
    unique_req_contents = {}  # {hash: file_path}
    skipped_count = 0

    for root, dirs, files in os.walk(OUTPUT_REPOS_DIR):
        if 'requirements.txt' in files:
            path = os.path.join(root, 'requirements.txt')
            with open(path, 'rb') as f:
                content = f.read()
            content_hash = hashlib.md5(content).hexdigest()

            # 我们只关心唯一内容的文件来进行预热
            if content_hash not in unique_req_contents:
                unique_req_contents[content_hash] = path
            else:
                skipped_count += 1

    req_files = list(unique_req_contents.values())
    print(f"Found {len(req_files) + skipped_count} requirements files.")
    print(f"Unique requirements sets: {len(req_files)} (Skipped {skipped_count} duplicates)")

    # 4. 逐个安装以预热缓存
    # [Fix] 模拟 evaluator.py 的行为：
    # 1. 对每个 requirements.txt 使用干净的 venv 环境 (避免共享环境导致的依赖版本锁定差异)
    # 2. 将 pytest 和 requirements.txt 一起安装 (确保解析结果一致)

    extra_index_args = []
    if sys.platform == "linux":
        # 使用 extra-index-url
        # [Change] 切换回官方 PyTorch CPU 源，虽然慢一点，但兼容性更好，且目录结构标准
        extra_index_args = ['--extra-index-url', 'https://download.pytorch.org/whl/cpu']

    success_count = 0
    fail_count = 0
    cached_count = 0
    download_count = 0

    for i, req_file in enumerate(req_files):
        rel_path = os.path.relpath(req_file, os.getcwd())
        # 显示当前正在处理的项目
        sys.stdout.write(f"\r[{i + 1}/{len(req_files)}] Processing {rel_path}...")
        sys.stdout.flush()

        # [Fix] 每次重建 venv，确保环境干净
        if os.path.exists(temp_venv):
            shutil.rmtree(temp_venv)

        # 创建 venv
        subprocess.run([uv_path, "venv", temp_venv], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

        # [Fix] 针对 Linux 环境处理 requirements.txt，强制使用 CPU 版 torch
        target_req_file = process_requirements_for_linux(req_file)

        # [Fix] 动态检测是否需要强制指定 torch CPU 版本 (保持与 evaluator 一致，为空)
        force_torch_cpu_args = []

        # [Fix] 针对 macOS 环境移除阿里云 CPU 源
        current_extra_index_args = extra_index_args
        if sys.platform != "linux":
            current_extra_index_args = []

        try:
            # Step 1: Install (Directly matches evaluator.py)
            # 关键：同时安装 requirements.txt 和 pytest

            install_cmd = [
                              uv_path, "pip", "install",
                              "-p", python_executable,
                              "-r", target_req_file,
                              "pytest",  # [Added] 添加 pytest，确保解析上下文一致
                              "--index-strategy", "unsafe-best-match"
                          ] + current_extra_index_args + force_torch_cpu_args

            install_result = subprocess.run(
                install_cmd,
                check=True,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.PIPE,
                text=True,
                timeout=600,  # 下载可能比较慢，给10分钟
                stdin=subprocess.DEVNULL
            )

            # 清理临时生成的 CPU requirements 文件
            if target_req_file != req_file and target_req_file.endswith(".cpu.tmp"):
                try:
                    os.remove(target_req_file)
                except OSError:
                    pass

            success_count += 1

            # 判断是否有下载行为
            logs = install_result.stderr

            if "Downloading" in logs:
                download_count += 1
                status = "⬇️  Download"
            else:
                cached_count += 1
                status = "⚡ Cached"

            # 完成后覆盖打印简要状态
            sys.stdout.write(
                f"\r[{i + 1}/{len(req_files)}] {status} | Cached: {cached_count} | Downloaded: {download_count} " + " " * 30)
            sys.stdout.flush()

        except subprocess.TimeoutExpired:
            print(f"\n  -> ⏳ Timeout in {rel_path}")
            fail_count += 1
        except subprocess.CalledProcessError as e:
            # 如果是 torch CPU 版本下载失败，尝试回退到标准版本（即 GPU 版，虽然大但能跑通）
            # 这是为了防止阻塞 CI/CD 流程
            if "unsatisfiable" in e.stderr and "torch" in e.stderr and sys.platform == "linux":
                print(f"\n  -> ⚠️ CPU Torch failed, falling back to standard torch in {rel_path}...")
                try:
                    # 使用原始 requirements.txt (不带 +cpu)
                    # 去掉 extra-index-url 防止再次解析错误 (或者保留也行，如果不匹配的话)
                    install_cmd_fallback = [
                        uv_path, "pip", "install",
                        "-p", python_executable,
                        "-r", req_file,  # Use original file
                        "pytest",
                        "--index-strategy", "unsafe-best-match"
                    ]  # No extra index args for fallback

                    subprocess.run(
                        install_cmd_fallback,
                        check=True,
                        stdout=subprocess.DEVNULL,
                        stderr=subprocess.PIPE,
                        text=True,
                        timeout=600,
                        stdin=subprocess.DEVNULL
                    )
                    success_count += 1
                    download_count += 1  # Assume download happened
                    sys.stdout.write(
                        f"\r[{i + 1}/{len(req_files)}] ⬇️  Fallback | Cached: {cached_count} | Downloaded: {download_count} " + " " * 30)
                    sys.stdout.flush()
                    continue  # Skip failure reporting
                except Exception as fallback_e:
                    pass  # Let it fail below

            print(f"\n  -> ❌ Failed in {rel_path}")
            if e.stderr:
                print(f"--- STDERR ---\n{e.stderr.strip()}")
            fail_count += 1

    print("\n\n--- Warmup Finished ---")
    print(f"Total Repos: {len(req_files)}")
    print(f"Fully Cached Repos: {cached_count}")
    print(f"Repos with Downloads: {download_count}")
    print(f"Failures: {fail_count}")
    print("UV cache (metadata & wheels) is now populated.")

    # 5. 清理临时文件
    print("Cleaning up...")
    if os.path.exists(temp_venv):
        shutil.rmtree(temp_venv)

    if fail_count > 0:
        print(f"Error: {fail_count} repositories failed to warm up.")
        sys.exit(1)


if __name__ == "__main__":
    main()
