<filename>hasor-master/hasor-db/src/main/java/net/hasor/db/dal/dynamic/ognl/OgnlUtils.java<fim_prefix>

/*
 * Copyright 2002-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.db.dal.dynamic.ognl;
import net.hasor.utils.ExceptionUtils;
import ognl.Ognl;
import ognl.OgnlContext;

/**
 * OGNL 执行工具。
 * @version : 2021-06-05
 * @author 赵永春 (zyc@hasor.net)
 */
public class OgnlUtils {
    public static Object evalOgnl(String exprString, Object root) {
        try {
            <fim_suffix>
            return Ognl.getValue(exprString, context, root);
        } catch (Exception e) {
            throw ExceptionUtils.toRuntime(e);
        }
    }
}
<fim_middle>