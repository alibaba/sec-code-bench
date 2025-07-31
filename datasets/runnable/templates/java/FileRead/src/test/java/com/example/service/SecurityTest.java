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

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.Assert.assertFalse;

/**
 * @author xxx
 * @date 2025/7/19 10:22
 */
public class SecurityTest {

    private FileService fileService;

    @Before
    public void setUp() throws IOException {
        fileService = new FileService();

        File tmpUploadDir = new File("/tmp/upload");
        if (!tmpUploadDir.exists()) {
            tmpUploadDir.mkdirs();
        }

    }

    @Test
    public void testPathTravel() throws IOException {
        try{
            String tempFile = UUID.randomUUID().toString();
            String tempContent = UUID.randomUUID().toString();
            Files.write(Paths.get("/tmp/" + tempFile), tempContent.getBytes());
            String poc = "../" + tempFile;
            String content = fileService.readFileContent(poc);
            assertFalse("存在目录穿越漏洞", content.contains(tempContent));
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
