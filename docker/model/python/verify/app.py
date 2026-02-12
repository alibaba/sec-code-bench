import os
import io
import shutil
import time
from datetime import datetime
from flask import Flask, jsonify, request
from core.evaluator import (
    setup_temp_directory,
    get_source_file_path_from_signature,
    parse_llm_response,
    setup_venv_and_install_deps,
    run_all_tests
)

from log_sdk import logger

app = Flask(__name__)
# 设定一个安全的基础目录，所有文件访问都必须在此目录下
SAFE_DIRECTORY = os.path.abspath(os.getcwd())

# 预定义的 Token，实际生产环境应从环境变量获取
API_TOKEN = os.environ.get('API_TOKEN', 'local-eval-token')

# --- Repository Mapping ---
REPO_MAP = {}
# Try to find the repos directory
if os.path.exists(os.path.join(SAFE_DIRECTORY, 'python_bench')):
    OUTPUT_REPOS_DIR = os.path.join(SAFE_DIRECTORY, 'python_bench')
else:
    OUTPUT_REPOS_DIR = os.path.join(SAFE_DIRECTORY, 'output_repos')

def build_repo_map():
    global REPO_MAP
    if not os.path.exists(OUTPUT_REPOS_DIR):
        print(f"Warning: {OUTPUT_REPOS_DIR} does not exist.")
        return

    print(f"Building repository map from {OUTPUT_REPOS_DIR}...")
    
    # Check if we are using the new python_bench structure (no language layer)
    is_python_bench = 'python_bench' in OUTPUT_REPOS_DIR
    
    items = os.listdir(OUTPUT_REPOS_DIR)
    
    for item in items:
        item_path = os.path.join(OUTPUT_REPOS_DIR, item)
        if not os.path.isdir(item_path): continue
        
        # If using python_bench, 'item' is category. 
        # If using output_repos, 'item' is language (e.g. python).
        
        if is_python_bench:
            # Structure: python_bench/Category/RepoName
            category = item
            cat_path = item_path
            
            for repo_name in os.listdir(cat_path):
                repo_path = os.path.join(cat_path, repo_name)
                if not os.path.isdir(repo_path): continue
                
                if os.path.exists(os.path.join(repo_path, 'signature.json')):
                    unique_id = f"{category}/{repo_name}"
                    REPO_MAP[unique_id] = repo_path
        else:
            # Structure: output_repos/Language/Category/RepoName
            # item is language
            lang_path = item_path
            for category in os.listdir(lang_path):
                cat_path = os.path.join(lang_path, category)
                if not os.path.isdir(cat_path): continue
                
                for repo_name in os.listdir(cat_path):
                    repo_path = os.path.join(cat_path, repo_name)
                    if not os.path.isdir(repo_path): continue
                    
                    if os.path.exists(os.path.join(repo_path, 'signature.json')):
                        unique_id = f"{category}/{repo_name}"
                        REPO_MAP[unique_id] = repo_path
    
    print(f"Loaded {len(REPO_MAP)} repositories.")

build_repo_map()

@app.route('/health')
def health_check():
    return jsonify({'status': 'ok'})

@app.route('/verify/<path:testcase>', methods=['POST'])
def verify_code(testcase):
    """
    在线评测接口
    接收 code
    返回 stdout, stderr, test_result, succ_rate
    """
    # Start timer
    start_time = time.time()

    data = request.json
    
    # Extract additional fields for logging
    prompt = data.get('prompt')
    prompt_path = data.get('prompt_path')
    model_name = data.get('model_name')
    thinking = data.get('thinking')

    # Token Authentication
    token = data.get('token')
    if token != API_TOKEN:
        return jsonify({'error': 'Unauthorized: Invalid token'}), 401

    repo_path = REPO_MAP.get(testcase)
    if not repo_path:
        return jsonify({'error': f'Testcase {testcase} not found.'}), 404
        
    code = data.get('code')
    
    if not code:
        return jsonify({'error': 'No code provided.'}), 400
        
    target_src_file = get_source_file_path_from_signature(repo_path)
    if not target_src_file:
         return jsonify({'error': 'Could not determine target source file from signature.'}), 500

    # Try to parse as XML first
    gen_path, gen_code = parse_llm_response(code)
    
    if gen_path and gen_code:
        final_code = gen_code
    else:
        final_code = code

    temp_project_path = None
    try:
        temp_project_path = setup_temp_directory(repo_path, target_src_file)
        
        full_gen_path = os.path.join(temp_project_path, target_src_file)
        os.makedirs(os.path.dirname(full_gen_path), exist_ok=True)
        with io.open(full_gen_path, 'w', encoding='utf-8') as f:
            f.write(final_code)
            
        python_executable = setup_venv_and_install_deps(temp_project_path)

        func_stats, sec_stats, function_output, security_output, failed_tests = run_all_tests(python_executable, temp_project_path)

        # 合并输出为统一字段（与其他 verifier 保持一致）
        combined_stdout = f"=== Function Test Output ===\n{function_output}\n\n=== Security Test Output ===\n{security_output}"

        result = {
            "stdout": combined_stdout.strip(),
            "stderr": "",
            "function_stdout": function_output,
            "function_stderr": "",
            "security_stdout": security_output,
            "security_stderr": "",
            "test_result": {
                "functional_result": {
                    "total_tests": func_stats['total'],
                    "total_failures": func_stats['failures'],
                    "total_errors": func_stats['errors'],
                    "total_skipped": func_stats['skipped']
                },
                "security_result": {
                    "total_tests": sec_stats['total'],
                    "total_failures": sec_stats['failures'],
                    "total_errors": sec_stats['errors'],
                    "total_skipped": sec_stats['skipped']
                },
                "failed_tests": failed_tests # 记录具体失败的用例名
            },
            "succ_rate": 0.0
        }
        
        total = func_stats['total'] + sec_stats['total']
        passed = func_stats['passed'] + sec_stats['passed']
        
        if total > 0:
            result['succ_rate'] = passed / total
        
        succ_rate = result.get('succ_rate', 0.0)
        duration = time.time() - start_time

        # 本地日志记录
        logger.info(f"Testcase: {testcase}, Success Rate: {succ_rate:.2%}, Duration: {duration:.2f}s, Model: {model_name}")

        return jsonify(result)

    except Exception as e:
        print(f"Error during verification: {e}")
        return jsonify({'error': str(e), 'stdout': str(e)}), 500
    finally:
        if temp_project_path and os.path.exists(temp_project_path):
            shutil.rmtree(temp_project_path)

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    app.run(debug=True, host='0.0.0.0', port=port)
