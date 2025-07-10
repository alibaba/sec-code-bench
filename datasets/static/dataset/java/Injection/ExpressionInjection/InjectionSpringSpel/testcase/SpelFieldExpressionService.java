<filename>coremedia-headless-server-master/headless-server-lib/src/main/java/com/coremedia/caas/service/expression/spel/schema/SpelFieldExpressionService.java<fim_prefix>

        package com.coremedia.caas.service.expression.spel.schema;

import com.coremedia.caas.service.expression.FieldExpression;
import com.coremedia.caas.service.expression.FieldExpressionCompiler;
import com.coremedia.caas.service.expression.FieldExpressionEvaluator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class SpelFieldExpressionService implements FieldExpressionCompiler, FieldExpressionEvaluator<Expression> {

    private static final Logger LOG = LoggerFactory.getLogger(SpelFieldExpressionService.class);


    private EvaluationContext evaluationContext;

    private SpelParserConfiguration parserConfiguration;
    private SpelExpressionParser expressionParser;


    public SpelFieldExpressionService(EvaluationContext evaluationContext) {
        this.evaluationContext = evaluationContext;
        // create parser with custom configurtation
        parserConfiguration = new SpelParserConfiguration(SpelCompilerMode.OFF, SpelFieldExpressionService.class.getClassLoader());
        expressionParser = new SpelExpressionParser(parserConfiguration);
    }


    @Override
    public FieldExpression compile(String pathExpression) {
        try {
            return new FieldExpression<>(expressionParser.parseExpression(pathExpression), this);
        } catch (Exception e) {
            LOG.error("Expression \"{}\" has errors: {}", pathExpression, e.getMessage());
            throw e;
        }
    }


    @Override
    public Object fetch(Expression expression, Object source) {
        <fim_suffix>
        return expression.getValue(evaluationContext, source);
    }

    @Override
    public <E> E fetch(Expression expression, Object source, Class<E> targetClass) {
        return expression.getValue(evaluationContext, source, targetClass);
    }
}
<fim_middle>