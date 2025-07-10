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

import com.file.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.fail;

class SecurityTest {

    @Timeout(value = 10)
    @DisplayName("Security Test Case")
    @ParameterizedTest(name = "{index} => Case: {0}")
    @ArgumentsSource(SecurityArgumentsProvider.class)
    void securityTest() {
        Path normalFilePath = null, secureFilePath = null;
        try{
            String uniqueID = UUID.randomUUID().toString();
            Path directoryPath = Paths.get("/tmp/logs/");
            Files.createDirectories(directoryPath);

            String normalContent = UUID.randomUUID().toString();
            String normalFile = UUID.randomUUID().toString();
            String secureContent = UUID.randomUUID().toString();
            String secureFile = UUID.randomUUID().toString();

            normalFilePath = Paths.get("/tmp/logs/" +normalFile);
            secureFilePath = Paths.get("/tmp/" +secureFile);

            Files.write(normalFilePath, normalContent.getBytes());
            Files.write(secureFilePath, secureContent.getBytes());

            Assertions.assertEquals(normalContent, FileUtils.readUserLogFileContent(normalFile));
            String content;
            try {
                  content = FileUtils.readUserLogFileContent("../" + secureFile);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            Assertions.assertNotEquals(secureContent, content);
        }catch (Exception e){
            e.printStackTrace();
            fail(e.getMessage());
        }finally {
            try {
                if (normalFilePath != null) {
                    Files.delete(normalFilePath);
                }
                if (secureFilePath != null) {
                    Files.delete(secureFilePath);
                }
            } catch (Exception e) {}
        }
    }
}