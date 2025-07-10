<filename>bootplus-master/src/main/java/io/github/frame/prj/validate/ValidateONGL.java<fim_prefix>

package io.github.frame.prj.validate;

import lombok.extern.slf4j.Slf4j;
import ognl.MemberAccess;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;

import java.lang.reflect.Member;
import java.util.Map;

/**
 * ONGL表达式工具类
 *
 * @author Updated by 思伟 on 2020/8/6
 */
@Slf4j
public class ValidateONGL {

    private static DefaultMemberAccess memberAccess = new DefaultMemberAccess();

    /**
     * ONGL表达式解析
     *
     * @param obj        任意源对象
     * @param expression 表达式
     * @return 解析后的值
     */
    public static Object getValue(Object obj, String expression) {
        if (null == obj || null == expression) {
            return null;
        }
        try {
            <fim_suffix>

            context.setRoot(obj);
            return Ognl.getValue(expression, context, context.getRoot());
        } catch (OgnlException e) {
            log.error("ONGL表达式解析错误[{}],跳过...", e.getMessage());
        }
        return null;
    }

    public static class DefaultMemberAccess implements MemberAccess {
        @Override
        public Object setup(Map context, Object target, Member member, String propertyName) {
            return null;
        }

        @Override
        public void restore(Map context, Object target, Member member, String propertyName, Object state) {

        }

        @Override
        public boolean isAccessible(Map context, Object target, Member member, String propertyName) {
            return true;
        }
    }

    public static void main(String[] args) {
        String str = "test ongl";
        System.out.println(getValue(str, "toString"));
        System.out.println(getValue(str, "bytes"));
        System.out.println(getValue(str, null));
    }

}
<fim_middle>