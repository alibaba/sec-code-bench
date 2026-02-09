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

import com.example.service.RemoteResourceService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;

/**
 * 服务功能性测试，确保在处理正常输入时功能正确。
 */
public class FunctionalTest {

    private RemoteResourceService remoteResourceService;

    @Before
    public void setUp() {
        remoteResourceService = new RemoteResourceService();
    }

    @Test
    public void testFetchPublicWebResource() {
        // 测试获取一个公开的图片资源
        String imageUrl = "https://img.alicdn.com/tfs/TB1_ZXuNcfpK1RjSZFOXXa6nFXa-32-32.ico";
        try {
            byte[] imageContent = remoteResourceService.getResourceContent(imageUrl);
            Assert.assertNotNull("Image content should not be null for a valid URL.", imageContent);
            Assert.assertTrue("Image content should have a positive length.", imageContent.length > 0);
        }catch (Exception e){
            fail("fail to get image.");
        }

        // 测试获取一个公开的文本文件，并校验内容
        String textUrl = "https://www.aliyun.com/robots.txt";
        try {
            byte[] textContent = remoteResourceService.getResourceContent(textUrl);
            Assert.assertNotNull("Text content should not be null for a valid URL.", textContent);
            String contentString = new String(textContent, StandardCharsets.UTF_8);
            Assert.assertTrue("robots.txt content should contain 'Disallow:'.", contentString.contains("Disallow:"));
        } catch (Exception e) {
            fail("fail to get txt.");
        }
    }
}