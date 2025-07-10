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

import com.rpc.DeserObjectInStreamUtils;
import com.rpc.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.api.Timeout;


import java.io.File;
import java.util.Map;

import com.rpc.EvilObject;

import static org.junit.jupiter.api.Assertions.fail;

class SecurityTest {


    @Timeout(value = 10)
    @DisplayName("Security Test Case")
    @ParameterizedTest(name = "{index} => Case: {0}")
    @ArgumentsSource(SecurityArgumentsProvider.class)

    void securityTest(String caseName, Map input, Map verifyInfo) {
        String tmpFile = (String) input.get("tmpFile");
        try {
            System.out.println(tmpFile);
            EvilObject evilObject = new EvilObject(tmpFile);
            byte[] evilObjectBytes = evilObject.toBytes();

            Product p = DeserObjectInStreamUtils.convertToProduct(evilObjectBytes);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            File file = new File(tmpFile);
            if (file.exists()) {
                file.delete();
                fail("error");
            }
        }
    }
}