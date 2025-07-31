// Copyright (c) 2025 Alibaba Group and its affiliates

// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at

//     http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.example.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件服务类，提供根据文件模式查找文件的功能
 */
public class FileService {

    /**
     * 查找符合指定模式的文件
     *
     * @param searchDir   搜索目录路径
     * @param filePattern 文件名模式（支持通配符）
     * @return 找到的文件列表，每个文件占一行；查找失败时返回错误信息
     */
    public String findFilesWithPattern(String searchDir, String filePattern) {
        if (searchDir == null || filePattern == null || searchDir.trim().isEmpty()) {
            return "错误：搜索目录或文件模式为空。";
        }

        // 构建 find 命令
        List<String> command = new ArrayList<>();
        command.add("bash");
        command.add("-c");
        // 命令说明：
        // - type f：只查找文件
        // - name：按名称匹配模式
        // - limit 10：最多返回10个文件
        String cmdStr = String.format("find \"%s\" -type f -name \"%s\" | head -n 10", searchDir, filePattern);
        command.add(cmdStr);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // 合并标准输出和错误输出

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(exitCode == 0 ? process.getInputStream() : process.getErrorStream()));

            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }

            reader.close();

            if (exitCode != 0) {
                return "错误：文件查找失败。\n" + result.toString();
            }

            if (result.length() == 0) {
                return "未找到匹配的文件。";
            }

            return result.toString().trim(); // 去除末尾换行
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt(); // 重新设置中断标志
            return "错误：执行命令时发生异常：" + e.getMessage();
        }
    }

    /**
     * 主方法用于测试
     */
    public static void main(String[] args) {
        FileService service = new FileService();

        // 示例1：查找所有 .txt 文件
        System.out.println("=== 查找所有 .txt 文件 ===");
        System.out.println(service.findFilesWithPattern("/home/user/documents", "*.txt"));

        // 示例2：查找所有 .java 文件
        System.out.println("\n=== 查找所有 .java 文件 ===");
        System.out.println(service.findFilesWithPattern("/project/src", "*.java"));

        // 示例3：查找特定文件
        System.out.println("\n=== 查找特定文件 ===");
        System.out.println(service.findFilesWithPattern("/etc", "passwd"));
    }
}