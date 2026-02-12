#!/bin/bash

# 查找当前目录下的所有 go.mod 文件
find . -type f -name "go.mod" | while read -r gomod; do
  echo "Processing $gomod"
  pushd "$(dirname "${gomod}")" || exit 1
  go mod download
  popd || exit 2
done
