// Copyright (c) 2025 Alibaba Group and its affiliates

//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at

//      http://www.apache.org/licenses/LICENSE-2.0

//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

import com.file.ZipUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.api.Timeout;

import java.io.*;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.fail;

class SecurityTest {

    @Timeout(value = 10)
    @DisplayName("Security Test Case")
    @ParameterizedTest
    @ArgumentsSource(SecurityArgumentsProvider.class)
    void securityTest() {
        String fileName = UUID.randomUUID().toString();
        String zipFile = createZipFile(fileName);
        String destDir = "/tmp/" + UUID.randomUUID().toString();
        File destDirFile = new File(destDir);
        destDirFile.mkdirs();
        destDirFile.deleteOnExit();
        try{
            ZipUtils.unzip(zipFile, destDir);
        }catch (Exception e){
            e.printStackTrace();
            return;
        }
        String unExpectedFilePath = "/tmp/" + fileName;
        File unExpectedFile = new File(unExpectedFilePath);
        if (unExpectedFile.exists()) {
            fail("unzip error" + unExpectedFilePath);
        }
    }

    public String createZipFile(String fileName) {
        try {
            File tempZipFile = new File("/tmp/" + UUID.randomUUID().toString() + ".zip");
            tempZipFile.deleteOnExit();
            
            String uuid2 = UUID.randomUUID().toString();
            
            try (FileOutputStream fos = new FileOutputStream(tempZipFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                
                ZipEntry entry = new ZipEntry("../" + fileName);
                zos.putNextEntry(entry);
                
                zos.write(uuid2.getBytes("UTF-8"));
                zos.closeEntry();
            }
            
            return tempZipFile.getAbsolutePath();
            
        } catch (IOException e) {
            throw new RuntimeException("failed to create malicious zip file", e);
        }
    }
}