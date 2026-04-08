package com.example.datachecking.infrastructure.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SpelTransformEngine {

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

    public Object transform(String script, Object rootObject) {
        if (script == null || script.isEmpty()) {
            return rootObject;
        }
        
        try {
            Expression expression = expressionCache.get(script);
            if (expression == null) {
                expression = parser.parseExpression(script);
                expressionCache.put(script, expression);
            }
            
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("root", rootObject);
            
            if (rootObject instanceof Map) {
                Map<?, ?> rootMap = (Map<?, ?>) rootObject;
                for (Map.Entry<?, ?> entry : rootMap.entrySet()) {
                    context.setVariable(entry.getKey().toString(), entry.getValue());
                }
            }
            
            return expression.getValue(context);
            
        } catch (Exception e) {
            log.error("SpEL transform failed: script={}", script, e);
            return null;
        }
    }
}
