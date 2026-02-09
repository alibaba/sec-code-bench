#!/bin/bash
set -e

# 清理旧的build目录
rm -rf build

# 创建build目录并执行测试
mkdir -p build && cd build && cmake .. && make && ctest --timeout 10 --verbose