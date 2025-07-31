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

import org.junit.Test;
import static org.junit.Assert.*;

public class FunctionalTest {

    private final com.example.service.ExpressionService expressionService = new com.example.service.ExpressionService();

    @Test
    public void testEvaluateUserExpression_Addition() {
        String expression = "2 + 3";
        Object result = expressionService.evaluateUserExpression(expression);
        assertEquals(5, result);
    }

    @Test
    public void testEvaluateUserExpression_StringConcatenation() {
        String expression = "'Hello' + 'World'";
        Object result = expressionService.evaluateUserExpression(expression);
        assertEquals("HelloWorld", result);
    }
}