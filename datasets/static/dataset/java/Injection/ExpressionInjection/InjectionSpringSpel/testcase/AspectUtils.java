<filename>redis-reactive-cache-master/src/main/java/com/vsware/libraries/redisreactivecache/util/AspectUtils.java<fim_prefix>

        package com.vsware.libraries.redisreactivecache.util;

import com.fasterxml.jackson.core.type.TypeReference;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

public class AspectUtils {

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    public String getKeyVal(JoinPoint joinPoint, String key, boolean useArgsHash) {
        String cacheKey = resolveKey(joinPoint, key);
        if (useArgsHash)
            return cacheKey + "_" + Arrays.hashCode(joinPoint.getArgs());
        else
        return cacheKey;

    }

    public Method getMethod(JoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        return methodSignature.getMethod();
    }

    public Type getMethodActualReturnType(Method method) {
        return ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
    }

    public TypeReference getTypeReference(Method method) {
        return new TypeReference<Object>() {
            @Override
            public Type getType() {
                return getMethodActualReturnType(method);
            }
        };
    }

    private String[] getParamNames(JoinPoint joinPoint) {
        CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();
        return codeSignature.getParameterNames();
    }

    private String resolveKey(JoinPoint joinPoint, String key) {
        if (StringUtils.hasText(key)) {
            if (key.contains("#") || key.contains("'")) {
            String[] parameterNames = getParamNames(joinPoint);
            Object[] args = joinPoint.getArgs();
<fim_suffix>
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
            return (String) expressionParser.parseExpression(key).getValue(context);
        }
        return key;
    }
        throw new RuntimeException("RedisReactiveCache annotation missing key");
}

}
<fim_middle>