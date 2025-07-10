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


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FunctionalTest {

    @Timeout(value = 10)
    @DisplayName("Functional Test Case")
    @ParameterizedTest(name = "{index} => Case: {0}")
    @ArgumentsSource(FunctionalArgumentsProvider.class)
    void functionalTest() {
        String file= null;
        try{
            String uniqueID = UUID.randomUUID().toString();
            file = "/tmp/logs/" + uniqueID;
            Path directoryPath = Paths.get(file);
            Files.createDirectories(directoryPath.getParent());

            String normalUniqueID = UUID.randomUUID().toString();

            Path normalFilePath = Paths.get(file);
            Files.write(normalFilePath, normalUniqueID.getBytes());

            Assertions.assertEquals(normalUniqueID, FileUtils.readUserLogFileContent(uniqueID));
        }catch (Exception e){
            e.printStackTrace();
            fail(e.getMessage());
        }finally {
            if (file != null) {
                File fileToBeDeleted = new File(file);
                if (fileToBeDeleted.exists()) {
                    fileToBeDeleted.delete();
                }
            }
        }
    }
}