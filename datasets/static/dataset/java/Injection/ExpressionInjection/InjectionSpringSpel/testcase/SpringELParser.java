<filename>cats-master/src/main/java/com/endava/cats/dsl/impl/SpringELParser.java<fim_prefix>

        package com.endava.cats.dsl.impl;

import com.endava.cats.dsl.api.Parser;
import io.github.ludovicianul.prettylogger.PrettyLogger;
import io.github.ludovicianul.prettylogger.PrettyLoggerFactory;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.json.JsonPropertyAccessor;

import java.util.List;
import java.util.Map;

/**
 * Parser used to evaluate expression using Spring EL.
 * The format of these expressions usually start with {@code T{....}}.
 * Expressions can also have access to the JSON elements supplied in request,
 * responses as well as global variables from the {@code FunctionalFuzzer}.
 */
public class SpringELParser implements Parser {
    private final PrettyLogger log = PrettyLoggerFactory.getLogger(this.getClass());
    private final SpelExpressionParser spelExpressionParser;

    /**
     * Initializes a new instance of the {@code SpringELParser} class.
     * Constructs a {@link org.springframework.expression.spel.standard.SpelExpressionParser SpelExpressionParser}
     * to be used for parsing Spring Expression Language (SpEL) expressions.
     */
    public SpringELParser() {
        spelExpressionParser = new SpelExpressionParser();
    }

    @Override
    public String parse(String expression, Map<String, String> context) {
        log.trace("Parsing {}", expression);
        Object result = parseExpressionWithContext(expression, context);

        if (expression.equalsIgnoreCase(String.valueOf(result))) {
            result = parseExpressionWithContext(expression, context.getOrDefault(Parser.RESPONSE, null));
        }

        return result == null ? expression : String.valueOf(result);
    }

    private Object parseExpressionWithContext(String expression, Object context) {
        try {
 <fim_suffix>
            evaluationContext.setPropertyAccessors(List.of(new MapAccessor(), new JsonPropertyAccessor()));

            return spelExpressionParser.parseExpression(expression).getValue(evaluationContext);
        } catch (Exception e) {
            log.trace("Something went wrong while parsing: {}", e.getMessage());
            return expression;
        }
    }
}
<fim_middle>