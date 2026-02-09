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

import com.example.service.DynamicMetricService;
import org.junit.Before;
import org.junit.Test;

import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.*;

/**
 * 服务功能性测试，确保在正常输入下，业务逻辑按预期工作。
 */
public class FunctionalTest {

    private DynamicMetricService metricService;

    @Before
    public void setUp() {
        metricService = new DynamicMetricService();
    }

    /**
     * 测试一个标准的业务场景：计算销售总额。
     * 公式: (revenue - cost) * (1 - taxRate)
     */
    @Test
    public void testNormalBusinessFormula() throws ScriptException {
        // 准备公式和上下文数据
        String formula = "(revenue - cost) * (1 - taxRate)";
        Map<String, Object> context = new HashMap<>();
        context.put("revenue", 50000.0);
        context.put("cost", 15000.0);
        context.put("taxRate", 0.15);

        try {
            // 执行计算
            Object result = metricService.calculate(formula, context);

            // 验证结果
            double expected = (50000.0 - 15000.0) * (1 - 0.15);
            assertEquals(expected, (Double) result, 0.001);
        } catch (Exception e) {
            fail("Failed to calculate formula: " + formula);
        }

    }

    /**
     * 测试包含特殊字符（如字符串）的条件逻辑公式。
     * 这有助于确保未来的修复方案不会错误地过滤掉合法的特殊字符。
     */
    @Test
    public void testFormulaWithConditionalLogicAndStrings() throws ScriptException {
        // 准备公式和上下文数据
        String formula = "kpi > 100 ? 'Excellent' : 'Good'";
        Map<String, Object> context = new HashMap<>();
        context.put("kpi", 120);

        try {
            // 执行计算
            Object result = metricService.calculate(formula, context);
            // 验证结果
            assertEquals("Excellent", result);
        } catch (Exception e) {
            fail("Failed to calculate formula: " + formula);
        }
    }
}