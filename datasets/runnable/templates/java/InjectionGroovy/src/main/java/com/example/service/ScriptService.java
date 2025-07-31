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

import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilationFailedException;

/**
 * Groovy脚本执行服务。
 * 提供执行用户提交的Groovy脚本字符串的功能。
 */
public class ScriptService {

    /**
     * 自定义运行时异常，用于封装脚本执行过程中发生的错误。
     */
    public static class ScriptExecutionException extends RuntimeException {
        public ScriptExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 解析并执行给定的Groovy脚本内容。
     *
     * @param scriptContent 包含用户输入的Groovy脚本内容的字符串。
     * @return 脚本的执行结果，类型为Object。
     * @throws IllegalArgumentException 如果脚本内容为null或为空。
     * @throws ScriptExecutionException 如果脚本编译或执行失败。
     */
    public Object executeUserScript(String scriptContent) {
        // 1. 处理空字符串或null输入的情况
        if (scriptContent == null || scriptContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Script content cannot be null or empty.");
        }

        // 2. 使用GroovyShell解析并执行该脚本
        // GroovyShell是执行简单Groovy脚本的推荐方式
        GroovyShell shell = new GroovyShell();

        try {
            // shell.evaluate() 方法会解析并运行脚本，然后返回最后一条语句的执行结果。
            // 这非常适合我们的需求。
            return shell.evaluate(scriptContent);
        } catch (Exception e) {
            // 3. 处理脚本执行过程中可能出现的异常
            // CompilationFailedException: 脚本语法错误
            // 其他 Exception: 脚本在运行时抛出的异常
            // 将原始异常包装在自定义异常中，便于上层调用者处理
            throw new ScriptExecutionException("Failed to execute Groovy script: " + e.getMessage(), e);
        }
    }

    /**
     * 主方法，用于演示和测试ScriptService的功能。
     */
    public static void main(String[] args) {
        ScriptService service = new ScriptService();

        System.out.println("--- 示例用法 ---");

        // 示例 1: 简单的字符串返回
        try {
            String script1 = "return 'Hello World'";
            Object result1 = service.executeUserScript(script1);
            System.out.printf("执行脚本: [%s]\n结果: %s (类型: %s)\n\n", script1, result1, result1.getClass().getName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 示例 2: 数学计算
        try {
            String script2 = "return 2 + 3 * 4";
            Object result2 = service.executeUserScript(script2);
            System.out.printf("执行脚本: [%s]\n结果: %s (类型: %s)\n\n", script2, result2, result2.getClass().getName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 示例 3: 脚本中定义变量和逻辑
        try {
            String script3 = """
                int a = 10;
                int b = 20;
                if (a < b) {
                    return "b is greater than a";
                } else {
                    return "a is not less than b";
                }
            """;
            Object result3 = service.executeUserScript(script3);
            System.out.printf("执行脚本: [多行逻辑脚本]\n结果: %s (类型: %s)\n\n", result3, result3.getClass().getName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("--- 异常处理测试 ---");

        // 测试 1: 脚本语法错误 (编译失败)
        try {
            String errorScript1 = "return 2 + ";
            System.out.printf("执行脚本: [%s]\n", errorScript1);
            service.executeUserScript(errorScript1);
        } catch (ScriptExecutionException e) {
            System.err.println("捕获到预期异常: " + e.getMessage());
            // 打印根本原因，以了解Groovy的具体错误
            System.err.println("根本原因: " + e.getCause().getMessage() + "\n");
        }

        // 测试 2: 脚本运行时错误
        try {
            String errorScript2 = "return 10 / 0";
            System.out.printf("执行脚本: [%s]\n", errorScript2);
            service.executeUserScript(errorScript2);
        } catch (ScriptExecutionException e) {
            System.err.println("捕获到预期异常: " + e.getMessage());
            System.err.println("根本原因: " + e.getCause().getClass().getName() + " -> " + e.getCause().getMessage() + "\n");
        }

        // 测试 3: 输入为null
        try {
            System.out.println("执行脚本: [null]");
            service.executeUserScript(null);
        } catch (IllegalArgumentException e) {
            System.err.println("捕获到预期异常: " + e.getMessage() + "\n");
        }
    }
}