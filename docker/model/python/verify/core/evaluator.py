import os
import io
import json
import shutil
import tempfile
import subprocess
import sys
import xml.etree.ElementTree as ET


def get_source_file_path_from_signature(repo_path):
    """从 signature.json 读取并构造待生成源文件的相对路径"""
    signature_path = os.path.join(repo_path, 'signature.json')
    if not os.path.exists(signature_path):
        return None

    with io.open(signature_path, 'r', encoding='utf-8') as f:
        signature = json.load(f)

    # 假设 src 目录结构与包名/模块名对应
    # Java: com.example.loader -> src/main/java/com/example/loader/
    # Python: config_loader -> src/config_loader/
    if signature.get('package_name'):  # Java
        package_path = signature['package_name'].replace('.', '/')
        file_name = signature.get('class_name', '') + '.java'
        return os.path.join('src', 'main', 'java', package_path, file_name)
    elif signature.get('module_name'):  # Python
        module_path = signature['module_name']
        file_name = module_path + '.py'
        return os.path.join('src', module_path, file_name)
    return None


def setup_temp_directory(repo_path, target_src_file):
    """创建临时目录，复制项目，并删除待生成的目标文件"""
    temp_dir = tempfile.mkdtemp()
    project_name = os.path.basename(repo_path)
    temp_project_path = os.path.join(temp_dir, project_name)
    shutil.copytree(repo_path, temp_project_path)

    file_to_delete = os.path.join(temp_project_path, target_src_file)
    if os.path.exists(file_to_delete):
        os.remove(file_to_delete)

    return temp_project_path


def parse_llm_response(response_text):
    """从LLM返回的自由格式文本中稳健地提取XML块并解析"""
    try:
        start_index = response_text.find('<result>')
        if start_index == -1:
            return None, None

        end_index = response_text.rfind('</result>')
        if end_index == -1:
            return None, None

        xml_content = response_text[start_index: end_index + len('</result>')]

        root = ET.fromstring(xml_content)
        path = root.find('.//path').text.strip()
        content = root.find('.//content').text
        return path, content
    except Exception as e:
        print(f"Error parsing LLM response: {e}")
        return None, None


def parse_junit_xml(xml_file):
    """
    智能解析junit.xml, 返回详细统计信息：total, passed, failures, errors, skipped
    """
    if not os.path.exists(xml_file):
        empty_stats = {'total': 0, 'passed': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
        return empty_stats, empty_stats

    try:
        tree = ET.parse(xml_file)
        root = tree.getroot()

        def get_stats(classname_filter):
            testcases = root.findall(f".//testcase[@classname='{classname_filter}']")
            total = len(testcases)
            failures = 0
            errors = 0
            skipped = 0
            failed_names = []

            for tc in testcases:
                if tc.find('failure') is not None:
                    failures += 1
                    failed_names.append(tc.get('name'))
                elif tc.find('error') is not None:
                    errors += 1
                    failed_names.append(tc.get('name'))
                elif tc.find('skipped') is not None:
                    skipped += 1

            passed = total - failures - errors - skipped
            return {
                "total": total,
                "passed": passed,
                "failures": failures,
                "errors": errors,
                "skipped": skipped
            }, failed_names

        func_stats, func_failed = get_stats('tests.test_functional')
        sec_stats, sec_failed = get_stats('tests.test_security')

        return func_stats, sec_stats, func_failed + sec_failed

    except Exception as e:
        print(f"Error parsing JUnit XML: {e}")
        empty_stats = {'total': 0, 'passed': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
        return empty_stats, empty_stats, []


def run_all_tests(python_executable, project_path):
    """运行项目中的所有测试，并返回结果和输出"""
    tests_dir = os.path.join(project_path, "tests")
    if not os.path.isdir(tests_dir):
        empty_stats = {'total': 0, 'passed': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
        return empty_stats, empty_stats, "", "", []

    # 分别运行功能测试和安全测试，以便分离输出
    func_test_file = os.path.join(tests_dir, "test_functional.py")
    sec_test_file = os.path.join(tests_dir, "test_security.py")

    func_junit_xml = os.path.join(project_path, "junit_functional.xml")
    sec_junit_xml = os.path.join(project_path, "junit_security.xml")

    function_output = ""
    security_output = ""
    func_stats = {'total': 0, 'passed': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
    sec_stats = {'total': 0, 'passed': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
    failed_tests = []

    # 1. 运行功能测试
    try:
        func_result = subprocess.run(
            [python_executable, '-m', 'pytest', func_test_file, f'--junitxml={func_junit_xml}'],
            cwd=project_path,
            capture_output=True,
            text=True,
            encoding='utf-8',
            timeout=25  # 每个测试最多25秒
        )
        function_output = func_result.stdout + "\n" + func_result.stderr
    except subprocess.TimeoutExpired as e:
        print(f"Functional test execution timed out: {e}")
        timeout_msg = f"Functional test execution timed out after {e.timeout} seconds."
        if e.stdout:
            stdout_str = e.stdout.decode('utf-8', errors='replace') if isinstance(e.stdout, bytes) else e.stdout
            function_output = f"{timeout_msg}\n\nLast Output:\n" + "\n".join(stdout_str.splitlines()[-20:])
        else:
            function_output = timeout_msg

    # 解析功能测试结果
    if os.path.exists(func_junit_xml):
        func_stats, _, func_failed = parse_junit_xml(func_junit_xml)
        failed_tests.extend(func_failed)

    # 2. 运行安全测试
    try:
        sec_result = subprocess.run(
            [python_executable, '-m', 'pytest', sec_test_file, f'--junitxml={sec_junit_xml}'],
            cwd=project_path,
            capture_output=True,
            text=True,
            encoding='utf-8',
            timeout=25  # 每个测试最多25秒
        )
        security_output = sec_result.stdout + "\n" + sec_result.stderr
    except subprocess.TimeoutExpired as e:
        print(f"Security test execution timed out: {e}")
        timeout_msg = f"Security test execution timed out after {e.timeout} seconds."
        if e.stdout:
            stdout_str = e.stdout.decode('utf-8', errors='replace') if isinstance(e.stdout, bytes) else e.stdout
            security_output = f"{timeout_msg}\n\nLast Output:\n" + "\n".join(stdout_str.splitlines()[-20:])
        else:
            security_output = timeout_msg

    # 解析安全测试结果
    if os.path.exists(sec_junit_xml):
        _, sec_stats, sec_failed = parse_junit_xml(sec_junit_xml)
        failed_tests.extend(sec_failed)

    return func_stats, sec_stats, function_output, security_output, failed_tests


def setup_venv_and_install_deps(project_path):
    """创建venv并安装依赖，优先使用uv加速，返回python可执行文件路径"""
    import time
    start_time = time.time()

    # 尝试导入 log_sdk，用于记录缓存命中情况
    try:
        from log_sdk import logger
    except ImportError:
        import logging
        logger = logging.getLogger(__name__)

    venv_path = os.path.join(project_path, 'venv')
    requirements_path = os.path.join(project_path, 'requirements.txt')

    # 检测是否安装了 uv
    uv_path = shutil.which("uv")
    use_uv = uv_path is not None

    import re

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
            # logger might not be initialized here if early return
            print(f"Warning: Failed to process requirements file {req_path}: {e}")
            return req_path

    # [Fix] 动态检测是否需要强制指定 torch CPU 版本 (不修改文件)
    force_torch_cpu_args = []

    # [Fix] 针对 Linux 环境处理 requirements.txt，强制使用 CPU 版 torch
    target_requirements_path = process_requirements_for_linux(requirements_path)

    if use_uv:
        # 1. 使用 uv 创建 venv (速度更快)
        try:
            subprocess.run([uv_path, 'venv', venv_path], cwd=project_path, check=True, capture_output=True)
        except subprocess.CalledProcessError:
            # 如果 uv venv 失败，回退到标准库
            subprocess.check_call([sys.executable, '-m', 'venv', venv_path], cwd=project_path,
                                  stdout=subprocess.DEVNULL)
    else:
        # 使用标准库创建 venv
        subprocess.check_call([sys.executable, '-m', 'venv', venv_path], cwd=project_path, stdout=subprocess.DEVNULL)

    if sys.platform == "win32":
        pip_executable = os.path.join(venv_path, 'Scripts', 'pip')
        python_executable = os.path.join(venv_path, 'Scripts', 'python')
    else:
        pip_executable = os.path.join(venv_path, 'bin', 'pip')
        python_executable = os.path.join(venv_path, 'bin', 'python')

    if os.path.exists(requirements_path):
        if use_uv:
            # 2. 使用 uv pip install (利用全局缓存和硬链接，极快)
            # -p 指定目标虚拟环境的 python 解释器
            # 回退 --offline，因为缓存可能不完整。恢复使用阿里云源 + 强制 CPU 版本，确保兜底下载速度快。

            # [Restored] 恢复 extra-index-url 但仅限 Linux
            # [Change] 切换回官方 PyTorch CPU 源
            extra_index_args = []
            if sys.platform == "linux":
                extra_index_args = ['--extra-index-url', 'https://download.pytorch.org/whl/cpu']

            # 同时安装 requirements.txt 和 pytest，减少一次 subprocess 调用，并确保环境完整
            # 增加 timeout 和 stdin=DEVNULL 以防止死锁卡死
            install_cmd = [
                              uv_path, 'pip', 'install',
                              '-p', python_executable,
                              '-r', target_requirements_path,
                              'pytest',
                              '--index-strategy', 'unsafe-best-match'  # [Fix] 允许跨源搜索
                          ] + extra_index_args + force_torch_cpu_args

            try:
                result = subprocess.run(
                    install_cmd,
                    cwd=project_path,
                    check=True,
                    capture_output=True,
                    text=True,
                    timeout=120,  # 防止安装无限卡死
                    stdin=subprocess.DEVNULL
                )

                # 清理临时生成的 CPU requirements 文件
                if target_requirements_path != requirements_path and target_requirements_path.endswith(".cpu.tmp"):
                    try:
                        os.remove(target_requirements_path)
                    except OSError:
                        pass

            except subprocess.TimeoutExpired as e:
                print(f"[Deps] Timeout installing dependencies for {os.path.basename(project_path)}")
                try:
                    logger.error({
                        "type": "dependency_install_error",
                        "project": os.path.basename(project_path),
                        "error_type": "timeout",
                        "timeout_seconds": 120,
                        "tool": "uv"
                    })
                except Exception:
                    pass
                raise e
            except subprocess.CalledProcessError as e:
                print(f"[Deps] Installation failed for {os.path.basename(project_path)}")
                print(f"--- STDERR ---\n{e.stderr}")
                try:
                    logger.error({
                        "type": "dependency_install_error",
                        "project": os.path.basename(project_path),
                        "error_type": "install_failed",
                        "exit_code": e.returncode,
                        "stderr": e.stderr[:1000] if e.stderr else "No stderr",  # 截取前1000字符
                        "tool": "uv"
                    })
                except Exception:
                    pass
                raise e

            # 分析日志判断是否命中缓存
            is_cache_hit = "Downloading" not in result.stderr
            status_icon = "⚡ Cached" if is_cache_hit else "⬇️  Downloading"
            duration = time.time() - start_time

            # 打印简明日志 (会显示在Docker/Gunicorn日志中)
            project_name = os.path.basename(project_path)
            log_msg = f"[Deps] {project_name} | {status_icon} | {duration:.2f}s | uv: True"
            print(log_msg)

            try:
                logger.info({
                    "type": "dependency_install",
                    "project": project_name,
                    "is_cache_hit": is_cache_hit,
                    "duration_seconds": duration,
                    "tool": "uv",
                    "stderr": result.stderr[:200]  # 只记录部分日志，避免过大
                })
            except Exception:
                pass  # 忽略日志记录错误

        else:
            # [Restored] 恢复 extra-index-url 但仅限 Linux
            # [Change] 切换回官方 PyTorch CPU 源
            extra_index_args = []
            if sys.platform == "linux":
                extra_index_args = ['--extra-index-url', 'https://download.pytorch.org/whl/cpu']

            subprocess.run(
                [
                    pip_executable, 'install',
                    '-r', target_requirements_path,
                ] + extra_index_args + force_torch_cpu_args,
                cwd=project_path,
                check=True,
                capture_output=True,
                text=True
            )

            # 清理临时文件
            if target_requirements_path.endswith(".cpu.tmp") and os.path.exists(target_requirements_path):
                try:
                    os.remove(target_requirements_path)
                except:
                    pass

    return python_executable
