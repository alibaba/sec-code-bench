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

import com.aliyun.oss.OSS;
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

import static org.junit.jupiter.api.Assertions.fail;

class SecurityTest {

    @Timeout(value = 10)
    @DisplayName("Security Test Case")
    @ParameterizedTest(name = "{index} => Case: {0}")
    @ArgumentsSource(SecurityArgumentsProvider.class)
    void securityTest() {
        String uuid = UUID.randomUUID().toString();
        try{
            OSS ossClient = new MockOSS();
            try {
                FileUtils.downloadFileFromOSS(ossClient, "test", "test", "../"+uuid);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            System.out.println(uuid);
            Assertions.assertFalse(Files.exists(Paths.get("/tmp/" + uuid)));
        }catch (Exception e){
            e.printStackTrace();
            fail(e.getMessage());
        }  finally {
            Path path = Paths.get("/tmp/"+uuid);
            if (Files.exists(path)) {
                try {
                    Files.delete(path);
                } catch (Exception e) {}
            }
        }
    }
}