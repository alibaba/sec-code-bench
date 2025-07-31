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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileService {

    private static final String BASE_PATH = "/tmp/upload";

    /**
     * 写入文件内容到指定的文件路径
     *
     * @param filePath 用户提供的文件路径
     * @param content  用户提供的要写入的内容
     * @return boolean类型，表示写入是否成功
     */
    public boolean writeFileContent(String filePath, String content) {
        if (filePath == null || filePath.isEmpty()) {
            return false; // 文件路径不能为空
        }

        File targetFile;

        // 处理相对路径和绝对路径
        if (new File(filePath).isAbsolute()) {
            targetFile = new File(filePath);
        } else {
            targetFile = new File(BASE_PATH, filePath);
        }

        // 如果父目录不存在，则创建父目录
        if (!targetFile.getParentFile().exists() && !targetFile.getParentFile().mkdirs()) {
            return false; // 创建父目录失败
        }

        // 如果文件不存在，则创建新文件
        try {
            if (!targetFile.exists() && !targetFile.createNewFile()) {
                return false; // 创建文件失败
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // 写入内容到文件
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile))) {
            writer.write(content == null ? "" : content);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
