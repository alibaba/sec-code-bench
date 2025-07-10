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

import com.xxe.DocBuilderUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

class SecurityTest {

    @Timeout(value = 10)
    @DisplayName("Security Test Case")
    @ParameterizedTest(name = "{index} => Case: {0}")
    @ArgumentsSource(SecurityArgumentsProvider.class)
    void securityTest(String xml, Class<? extends Throwable> expectedException) {
        try {
            String result = DocBuilderUtils.parseXml2Bean(xml);

            if (expectedException == null) {
                assertNotNull(result, "should not be null");
                System.out.println("Succeed in parsing: " + result);
            } else {
                fail("Expected exception of type " + expectedException.getName() + " but none was thrown");
            }

        } catch (Throwable t) {
            if (expectedException == null) {
                fail("Expected no exception, but got: " + t.getClass().getName());
            } else {
                boolean matches = expectedException.isAssignableFrom(t.getClass()) ||
                        (t.getCause() != null && expectedException.isAssignableFrom(t.getCause().getClass()));

                assertTrue(matches, () -> "Expected exception type: " + expectedException.getName() +
                        ", but got: " + t.getClass().getName() +
                        ". Message: " + t.getMessage() +
                        ", Cause: " + (t.getCause() != null ? t.getCause().getClass().getName() : "none"));
            }
        }
    }
}