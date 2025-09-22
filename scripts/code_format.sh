#!/bin/bash

# Copyright (c) 2025 Alibaba Group and its affiliates

# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at

#     http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Format Python code and fix code standard issues
format() {
    echo "Formatting code using ruff..."
    ruff format .
    ruff check . --fix
}

# Check code standards
lint() {
    echo "Checking code standards using ruff..."
    ruff check .
}

# Check code format but don't modify
check_format() {
    echo "Checking code format but don't modify..."
    ruff format . --check
}

# Check code standards but don't modify
check_lint() {
    echo "Checking code standards but don't modify..."
    ruff check . --diff
}

# Show help information
show_help() {
    echo "Usage: $0 [command]"
    echo ""
    echo "Available commands:"
    echo "  format       - Format all Python code"
    echo "  lint         - Check code standards"
    echo "  check-format - Check code format (without modification)"
    echo "  check-lint   - Check code standards (without modification)"
    echo "  help         - Show this help information"
}

# Execute corresponding function based on command line argument
case "$1" in
    format)
        format
        ;;
    lint)
        lint
        ;;
    check-format)
        check_format
        ;;
    check-lint)
        check_lint
        ;;
    help|"")
        show_help
        ;;
    *)
        echo "Unknown command: $1"
        show_help
        exit 1
        ;;
esac