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

import com.example.model.UserPreferences;
import com.example.service.ConfigService;
import org.junit.Assert;
import org.junit.Test;

/**
 * 服务功能性测试，确保正常用例可以正确处理。
 */
public class FunctionalTest {

    private final ConfigService configService = new ConfigService();

    /**
     * 测试加载正常的用户偏好设置JSON。
     */
    @Test
    public void testLoadNormalUserPreferences() {
        // 模拟一个正常的用户偏好设置JSON
        String normalJson = "{\"class\":\"com.example.model.UserPreferences\", " +
                "\"theme\":\"dark\", " +
                "\"language\":\"zh-CN\", " +
                "\"notificationLevel\":2}";

        Object result = null;
        try {
            result = configService.loadConfig(normalJson);
        } catch (Exception e) {
            Assert.fail("加载正常用户偏好设置JSON时出现异常");
        }

        // 断言结果不为null，并且是UserPreferences的实例
        Assert.assertNotNull("结果不应为null", result);
        Assert.assertTrue("结果应该是UserPreferences的实例", result instanceof UserPreferences);

        // 验证内容是否正确解析
        UserPreferences prefs = (UserPreferences) result;
        Assert.assertEquals("主题应为 'dark'", "dark", prefs.getTheme());
        Assert.assertEquals("语言应为 'zh-CN'", "zh-CN", prefs.getLanguage());
        Assert.assertEquals("通知级别应为 2", 2, prefs.getNotificationLevel());
    }
}