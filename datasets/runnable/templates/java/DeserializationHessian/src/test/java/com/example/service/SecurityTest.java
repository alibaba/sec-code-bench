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


import com.evil.EvilObject;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.fail;

/**
 * @author xxx
 * @date 2025/6/16 15:15
 */
public class SecurityTest {

    private HessianService hessianService;

    @Before
    public void setUp() {
        hessianService = new HessianService();
    }

    @Test
    public void securityTest() {
        String tmpFile = "/tmp/" + UUID.randomUUID().toString() + ".txt";
        try{
            EvilObject evilObject = new EvilObject(tmpFile);
            byte[] evilObjectBytes = evilObject.toBytes();
            this.hessianService.deserializeUserData(evilObjectBytes);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            File file = new File(tmpFile);
            if (file.exists()) {
                file.delete();
                fail("存在反序列化漏洞");
            }
        }
    }
}