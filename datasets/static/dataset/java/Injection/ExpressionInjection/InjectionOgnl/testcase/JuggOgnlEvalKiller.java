<filename>jugg-master/src/main/java/com/xhinliang/jugg/parse/ognl/JuggOgnlEvalKiller.java<fim_prefix>

package com.xhinliang.jugg.parse.ognl;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import javax.annotation.Nonnull;

import com.xhinliang.jugg.loader.IBeanLoader;
import com.xhinliang.jugg.parse.IJuggEvalKiller;

import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;

/**
 * @author xhinliang
 */
public class JuggOgnlEvalKiller implements IJuggEvalKiller {

    private OgnlContext globalContext;

    private Function<String, JuggOgnlContext> localContextSupplier;

    public JuggOgnlEvalKiller(IBeanLoader beanLoader) {
        this.globalContext = new JuggOgnlContext(beanLoader);
        this.localContextSupplier = new Function<String, JuggOgnlContext>() {

            private ConcurrentMap<String, JuggOgnlContext> contextMap = new ConcurrentHashMap<>();

            @Override
            public JuggOgnlContext apply(String commandContext) {
                return contextMap.computeIfAbsent(commandContext, (key) -> new JuggOgnlContext(beanLoader));
            }
        };
    }

    @Override
    public Object eval(String command, String username) {
        try {
            <fim_suffix>

        } catch (OgnlException e) {
            throw new RuntimeException(firstNonNull(e.getReason(), e));
        }
    }

    @Nonnull
    @Override
    public Map getContext(String username) {
        return localContextSupplier.apply(username);
    }
}
<fim_middle>