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

# Install all Java Maven dependencies for the project
# This script finds all pom.xml files in the project and runs mvn dependency:go-offline
# to download all required dependencies

set -e  # Exit on any error

# Check if datasets directory exists
if [ ! -d "datasets" ]; then
    echo "❌ Error: 'datasets' directory not found."
    echo "Please run this script from the project root directory."
    exit 1
fi

# Function to install dependencies for a given pom.xml file
install_deps() {
    local POM_FILE="$1"
    local TARGET_DIR="$(dirname "$POM_FILE")"
    
    # Enter the directory where the pom.xml is located
    cd "$TARGET_DIR" || {
        echo "❌ Failed to cd into $TARGET_DIR" >&2
        exit 1
    }
    
    echo "🚀 Running mvn dependency:go-offline in $(pwd)"
    
    # Execute the command, and if it fails, output an error
    mvn dependency:go-offline || {
        echo "❌ Failed in $TARGET_DIR" >&2
        exit 1
    }
    
    # Return to the original directory
    cd - > /dev/null
}

# Export the function so it can be used by xargs
export -f install_deps
export -p

echo "🔍 Finding all pom.xml files under datasets/templates..."

# Find all pom.xml files and run mvn dependency:go-offline in parallel
find datasets/templates -name pom.xml -print0 | \
xargs -0 -P 8 -n 1 bash -c 'install_deps "$@"' _

if [ $? -eq 0 ]; then
    echo "✅ All Java Maven dependencies installed successfully"
else
    echo "❌ Failed to install Java Maven dependencies"
    exit 1
fi

echo "🎉 Java dependency installation completed!"