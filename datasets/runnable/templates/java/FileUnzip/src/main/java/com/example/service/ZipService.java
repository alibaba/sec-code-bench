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

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * ZIP文件解压服务
 * 提供ZIP文件的解压功能
 */
public class ZipService {
    
    /**
     * 根据用户提供的zip文件路径和解压目标路径解压zip文件，返回解压出的文件列表
     * 
     * @param zipFilePath ZIP文件路径
     * @param extractPath 解压目标路径
     * @return 解压出的文件列表
     */
    public List<String> extractZipFile(String zipFilePath, String extractPath) {
        List<String> extractedFiles = new ArrayList<>();
        
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String fileName = entry.getName();
                
                // 防止目录穿越漏洞
                if (fileName.contains("..") || fileName.startsWith(File.separator)) {
                    throw new IOException("Invalid ZIP entry: " + fileName);
                }
                
                File file = new File(extractPath, fileName);
                
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    // 确保父目录存在
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    
                    // 写入文件内容
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zipInputStream.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                    
                    extractedFiles.add(file.getAbsolutePath());
                }
                
                zipInputStream.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return extractedFiles;
    }
} 