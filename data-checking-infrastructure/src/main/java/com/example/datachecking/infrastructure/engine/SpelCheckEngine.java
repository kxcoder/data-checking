package com.example.datachecking.infrastructure.engine;

import com.example.datachecking.domain.model.CheckExecutionResult;
import com.example.datachecking.domain.model.CheckRule;
import com.example.datachecking.domain.model.DataCheckMessage;
import com.example.datachecking.domain.service.CheckEngine;
import com.example.datachecking.domain.model.RuleType;
import com.example.datachecking.infrastructure.cache.CompiledExpressionCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * SpEL 预编译核对引擎
 * <p>
 * 职责: 使用预编译的 SpEL 表达式执行数据核对。
 * <p>
 * 性能优化: 表达式从 CompiledExpressionCache 获取（已预编译为字节码），
 * 避免每次核对时重复解析，性能提升约 100 倍。
 * <p>
 * SpEL 表达式中可访问的变量:
 * <ul>
 *   <li>#traceId — 链路追踪ID</li>
 *   <li>#timestamp — 方法调用时间戳</li>
 *   <li>#return / #result — 方法返回值</li>
 *   <li>#paramName — 方法入参（直接展开）</li>
 *   <li>#contextKey — 上下文扩展数据</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpelCheckEngine implements CheckEngine {

    /** 预编译表达式缓存 */
    private final CompiledExpressionCache expressionCache;

    @Override
    public RuleType getRuleType() {
        return RuleType.SPEL;
    }

    /**
     * 执行 SpEL 表达式核对
     * <p>
     * 流程: 获取预编译表达式 → 构建上下文 → 执行 → 返回结果
     *
     * @param rule    核对规则
     * @param message Kafka 消息数据
     * @return 执行结果（包含是否通过、耗时、失败原因）
     */
    @Override
    public CheckExecutionResult execute(CheckRule rule, DataCheckMessage message) {
        long startTime = System.nanoTime();

        try {
            // 从缓存获取预编译表达式（避免重复解析）
            Expression expression = expressionCache.getSpelExpression(rule.getId(), rule.getExpression());
            // 构建 SpEL 执行上下文
            StandardEvaluationContext context = createContext(message);

            // 执行表达式，期望返回 Boolean 类型
            Boolean result = expression.getValue(context, Boolean.class);
            long duration = System.nanoTime() - startTime;

            if (Boolean.TRUE.equals(result)) {
                return CheckExecutionResult.success(rule.getId(), rule.getMethodPattern(), message.getTraceId(), duration);
            } else {
                return CheckExecutionResult.failure(rule.getId(), rule.getMethodPattern(), message.getTraceId(), duration,
                        "SpEL表达式返回false");
            }
        } catch (Exception e) {
            long duration = System.nanoTime() - startTime;
            log.error("SpEL执行异常: ruleId={}, expression={}", rule.getId(), rule.getExpression(), e);
            return CheckExecutionResult.failure(rule.getId(), rule.getMethodPattern(), message.getTraceId(), duration,
                    "SpEL执行异常: " + e.getMessage());
        }
    }

    /**
     * 构建 SpEL 执行上下文
     * <p>
     * 注入以下变量供表达式访问:
     * #traceId, #timestamp, #return, #result, 以及所有入参和上下文数据
     */
    private StandardEvaluationContext createContext(DataCheckMessage message) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("traceId", message.getTraceId());
        context.setVariable("timestamp", message.getTimestamp());
        context.setVariable("return", message.getReturnData());
        context.setVariable("result", message.getReturnData());

        // 将入参直接展开为变量，表达式中可用 #paramName 访问
        if (message.getInputParams() != null) {
            message.getInputParams().forEach(context::setVariable);
        }
        // 将上下文数据展开为变量
        if (message.getContextData() != null) {
            message.getContextData().forEach(context::setVariable);
        }
        return context;
    }
}
