package com.example.datachecking.infrastructure.engine;

import com.example.datachecking.domain.model.CheckRule;
import com.example.datachecking.domain.model.DecisionResult;
import com.example.datachecking.domain.model.RuleType;
import com.example.datachecking.domain.service.DataSupplierService;
import com.example.datachecking.domain.service.MarketingDecisionExecutor;
import com.example.datachecking.domain.service.SceneRuleService;
import com.example.datachecking.infrastructure.cache.CompiledExpressionCache;
import com.example.datachecking.infrastructure.cache.CompiledGroovyScript;
import com.google.common.collect.Maps;
import com.google.common.base.Preconditions;
import groovy.lang.Binding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketingDecisionEngine implements MarketingDecisionExecutor {

    private final CompiledExpressionCache expressionCache;
    private final DataSupplierService dataSupplierService;
    private final SceneRuleService sceneRuleService;

    private static final long RULE_TIMEOUT_MS = 30;
    private static final int RULE_EXECUTION_THREADS = 4;

    private final ExecutorService ruleExecutor = Executors.newFixedThreadPool(RULE_EXECUTION_THREADS);

    public DecisionResult execute(String sceneCode, Long uid, Map<String, Object> params, Map<String, Object> context) {
        long startTime = System.nanoTime();
        Map<String, Object> ruleResults = Maps.newHashMap();
        Map<String, Object> outputData = Maps.newHashMap();
        String finalDecisionCode = "REJECT";
        String finalMessage = "No rules matched";

        try {
            List<CheckRule> rules = sceneRuleService.matchRules(sceneCode);
            if (rules == null || rules.isEmpty()) {
                return DecisionResult.builder()
                        .decisionCode("REJECT")
                        .message("No rules configured for scene: " + sceneCode)
                        .data(outputData)
                        .processTimeMs(0L)
                        .success(true)
                        .build();
            }

            Map<String, Object> variables = Maps.newHashMap();
            variables.put("uid", uid);
            variables.put("params", params);
            variables.put("context", context);
            variables.put("ruleResults", ruleResults);
            variables.put("supplier", new SupplierFunction(dataSupplierService));
        Preconditions.checkArgument(uid != null, "uid cannot be null");

            for (CheckRule rule : rules) {
                Object result = executeRule(rule, variables);
                String ruleKey = "rule_" + rule.getId();
                ruleResults.put(ruleKey, result);

                if (result instanceof Map) {
                    Map<String, Object> resultMap = (Map<String, Object>) result;
                    if (resultMap.containsKey("decision")) {
                        finalDecisionCode = String.valueOf(resultMap.get("decision"));
                        finalMessage = String.valueOf(resultMap.getOrDefault("reason", ""));
                        outputData.putAll(resultMap);
                        break;
                    }
                } else if (Boolean.TRUE.equals(result)) {
                    finalDecisionCode = "PASS";
                    finalMessage = "Rule passed: " + rule.getRuleName();
                }
            }

        } catch (Exception e) {
            log.error("Marketing decision execution failed: sceneCode={}", sceneCode, e);
            finalDecisionCode = "ERROR";
            finalMessage = e.getMessage();
        }

        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

        return DecisionResult.builder()
                .decisionCode(finalDecisionCode)
                .message(finalMessage)
                .data(outputData)
                .processTimeMs(durationMs)
                .success("ERROR".equals(finalDecisionCode) ? false : true)
                .build();
    }

    private Object executeRule(CheckRule rule, Map<String, Object> variables) {
        try {
            return switch (rule.getRuleType()) {
                case SPEL -> executeSpel(rule.getExpression(), variables);
                case GROOVY -> executeGroovy(rule.getExpression(), variables);
            };
        } catch (Exception e) {
            log.error("Rule execution failed: ruleId={}, ruleName={}", rule.getId(), rule.getRuleName(), e);
            return Map.of("decision", "ERROR", "reason", e.getMessage());
        }
    }

    private Object executeSpel(String expression, Map<String, Object> variables) {
        Expression exp = expressionCache.getSpelExpression(0L, expression);
        StandardEvaluationContext ctx = new StandardEvaluationContext();

        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            ctx.setVariable(entry.getKey(), entry.getValue());
        }

        try {
            return exp.getValue(ctx);
        } catch (Exception e) {
            log.error("SpEL evaluation failed: expression={}", expression, e);
            throw new RuntimeException("SpEL evaluation failed: " + e.getMessage(), e);
        }
    }

    private Object executeGroovy(String script, Map<String, Object> variables) {
        CompiledGroovyScript compiledScript = expressionCache.getGroovyScript(0L, script);
        Binding binding = new Binding();

        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            binding.setVariable(entry.getKey(), entry.getValue());
        }

        try {
            return compiledScript.execute(binding);
        } catch (Exception e) {
            log.error("Groovy execution failed: script={}", script, e);
            throw new RuntimeException("Groovy execution failed: " + e.getMessage(), e);
        }
    }

    public Object evaluateSpel(String expression, Map<String, Object> variables) {
        return executeSpel(expression, variables);
    }

    public Object evaluateGroovy(String script, Map<String, Object> variables) {
        return executeGroovy(script, variables);
    }

    @RequiredArgsConstructor
    public static class SupplierFunction {
        private final DataSupplierService dataSupplierService;

        public Object get(String key, Map<String, Object> params) {
            return dataSupplierService.supply(key, params);
        }

        public Object get(String key) {
            return dataSupplierService.supply(key, null);
        }
    }
}