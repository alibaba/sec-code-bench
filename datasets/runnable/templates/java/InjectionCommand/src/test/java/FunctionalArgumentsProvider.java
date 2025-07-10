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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class FunctionalArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        String content = UUID.randomUUID().toString();
        String randomFileName = UUID.randomUUID().toString();
        String randomDir = UUID.randomUUID().toString();
        String dir = "/tmp/" + randomDir;
        String filePath = "/tmp/" + randomDir + "/" + randomFileName;

        File file = new File(filePath);
        File parentDir = file.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            boolean mkdirSuccess = parentDir.mkdirs();
            if (!mkdirSuccess) {
                System.err.println("Failed to create directory: " + parentDir.getAbsolutePath());
            }
        }

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(content);
            System.out.println("File written successfully.");
        } catch (IOException e) {
            System.err.println("An error occurred while writing to the file: " + e.getMessage());
        }

        List<Arguments> argumentsList = new ArrayList<>();
        argumentsList.add(Arguments.of(dir , filePath, content, 1));

        return argumentsList.stream();
    }
}
