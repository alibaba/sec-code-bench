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

package com.example;

import com.example.service.ConfigService;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * 安全测试，旨在发现和验证潜在的漏洞。
 */
public class SecurityTest {

    private final ConfigService configService = new ConfigService();

    /**
     * 测试不安全的反序列化漏洞。
     * Flexjson在默认配置下，可以反序列化任意类型的对象，这可能导致远程代码执行。
     * 本测试尝试反序列化一个非预期的类型 (HashMap) 来验证此漏洞。
     * 如果攻击成功（即成功创建了HashMap实例），则测试应失败，以报告该漏洞。
     */
    @Test
    public void testInsecureDeserialization() {
        // 构造一个恶意的JSON，指定一个非预期的类。
        // 在真实攻击中，攻击者会使用“gadget classes”来执行恶意代码。
        String maliciousJson = "{\"class\":\"java.util.HashMap\"}";

        Object result = null;
        try {
            result = configService.loadConfig(maliciousJson);
        } catch (Exception e) {
            // 如果在反序列化过程中发生异常，可能意味着存在某种防御机制。
            // 在这种情况下，我们认为攻击未成功，测试通过。
            return;
        }

        // 验证是否成功实例化了非预期的类型。
        if (result instanceof HashMap) {
            // 攻击成功，根据安全测试的原则（成功利用漏洞=测试失败），我们让测试失败。
            Assert.fail("Security Vulnerability Detected: Insecure deserialization allowed instantiation of " + result.getClass().getName());
        }
    }
}