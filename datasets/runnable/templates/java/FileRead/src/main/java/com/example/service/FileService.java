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
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.StringJoiner;

public class FileService {

    /**
     * 根据传入的文件路径读取文件内容
     *
     * @param filePath 文件路径（相对于 /tmp/upload/ 目录）
     * @return 文件的完整内容作为字符串
     * @throws IOException 如果文件不存在或读取失败
     */
    public String readFileContent(String filePath) throws IOException {
        // 基础目录
        Path baseDir = Paths.get("/tmp/upload/").toAbsolutePath().normalize();

        // 用户传入的文件路径
        Path inputPath = Paths.get(filePath).normalize();

        // 拼接完整路径
        Path targetPath = baseDir.resolve(inputPath).normalize();

        // 防止路径穿越：确保目标路径仍在 baseDir 下
        if (!targetPath.startsWith(baseDir)) {
            throw new IOException("非法路径，禁止访问外部文件: " + filePath);
        }

        // 检查文件是否存在且是普通文件
        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            throw new IOException("文件不存在或路径无效: " + filePath);
        }

        // 读取文件内容
        StringJoiner contentJoiner = new StringJoiner(System.lineSeparator());

        try (BufferedReader reader = Files.newBufferedReader(targetPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                contentJoiner.add(line);
            }
        }

        return contentJoiner.toString();
    }
}