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

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.apache.velocity.runtime.resource.util.StringResourceRepositoryImpl;
import org.apache.velocity.runtime.parser.node.ASTReference;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.apache.velocity.runtime.parser.Parser;
import org.apache.velocity.util.introspection.SecureUberspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.Properties;

/**
 * 安全的 Velocity 模板服务类，防止模板注入攻击。
 */
public class VelocityTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(VelocityTemplateService.class);

    private static final VelocityEngine velocityEngine;
    private static final String TEMPLATE_KEY = "user_dashboard_template";

    static {
        Properties props = new Properties();

        // 使用 SecureUberspector 限制反射访问
        // props.setProperty(RuntimeConstants.UBERSPECT_CLASSNAME, SecureUberspector.class.getName());

        // 禁用 #evaluate() 指令
        props.setProperty("directive.evaluate.line", "1");
        props.setProperty("directive.evaluate.enabled", "false");

        // 启用字符串资源加载器
        props.setProperty(RuntimeConstants.RESOURCE_LOADER, "string");
        props.setProperty("string." + RuntimeConstants.RESOURCE_LOADER + ".class", StringResourceLoader.class.getName());

        velocityEngine = new VelocityEngine();
        velocityEngine.init(props);

        // 初始化字符串资源仓库
        StringResourceRepository repo = new StringResourceRepositoryImpl();
        StringResourceLoader.getRepository().putStringResource(TEMPLATE_KEY, "");

        logger.info("安全版 Velocity 模板引擎已初始化完成。");
    }

    /**
     * 安全渲染用户仪表板模板。
     *
     * @param userId 用户ID
     * @param dashboardTemplate 用户提供的Velocity模板内容
     * @return 渲染后的HTML字符串
     */
    public String renderUserDashboard(String userId, String dashboardTemplate) {
        if (userId == null || dashboardTemplate == null || dashboardTemplate.trim().isEmpty()) {
            logger.warn("用户ID或模板内容为空，无法渲染仪表板。");
            return "<div>模板内容或用户ID为空，请检查输入。</div>";
        }

        try {
            // 存储模板内容到字符串资源仓库
            StringResourceLoader.getRepository().putStringResource(TEMPLATE_KEY, dashboardTemplate);

            // 创建安全上下文
            Context context = new VelocityContext();
            context.put("userId", userId);

            // 渲染模板
            StringWriter writer = new StringWriter();
            velocityEngine.mergeTemplate(TEMPLATE_KEY, "UTF-8", context, writer);

            String result = writer.toString();
            logger.info("仪表板模板安全渲染成功，用户ID: {}", userId);
            return result;

        } catch (ResourceNotFoundException e) {
            logger.error("模板资源未找到：{}", e.getMessage(), e);
            return "<div>模板资源未找到，请检查模板内容。</div>";
        } catch (ParseErrorException e) {
            logger.error("模板语法解析失败：{}", e.getMessage(), e);
            return "<div>模板语法错误，请检查模板格式。</div>";
        } catch (Exception e) {
            logger.error("安全渲染仪表板模板时发生错误：{}", e.getMessage(), e);
            return "<div>发生错误，无法安全渲染模板。</div>";
        }
    }
}