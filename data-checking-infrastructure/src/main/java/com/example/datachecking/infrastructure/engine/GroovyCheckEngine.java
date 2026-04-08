package com.example.datachecking.infrastructure.engine;

import com.example.datachecking.domain.model.CheckExecutionResult;
import com.example.datachecking.domain.model.CheckRule;
import com.example.datachecking.domain.model.DataCheckMessage;
import com.example.datachecking.domain.service.CheckEngine;
import com.example.datachecking.domain.model.RuleType;
import com.example.datachecking.infrastructure.cache.CompiledExpressionCache;
import com.example.datachecking.infrastructure.cache.CompiledGroovyScript;
import groovy.lang.Binding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Groovy 预编译核对引擎
 * <p>
 * 职责: 使用预编译的 Groovy 脚本执行数据核对。
 * <p>
 * 与 SpEL 的区别:
 * <ul>
 *   <li>SpEL 适用于简单条件判断，轻量高效</li>
 *   <li>Groovy 适用于复杂业务逻辑，支持完整 Java 语法（循环、分支、集合操作等）</li>
 * </ul>
 * <p>
 * Groovy 脚本中可访问的内置变量:
 * <ul>
 *   <li>inputParams — 方法入参 (Map&lt;String, Object&gt;)</li>
 *   <li>returnData — 方法返回值 (Object)</li>
 *   <li>traceId — 链路追踪ID (String)</li>
 *   <li>timestamp — 方法调用时间戳 (Long)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroovyCheckEngine implements CheckEngine {

    /** 预编译脚本缓存 */
    private final CompiledExpressionCache expressionCache;

    @Override
    public RuleType getRuleType() {
        return RuleType.GROOVY;
    }

    /**
     * 执行 Groovy 脚本核对
     * <p>
     * 流程: 获取预编译脚本 → 创建 Binding → 执行 → 返回结果
     *
     * @param rule    核对规则
     * @param message Kafka 消息数据
     * @return 执行结果（包含是否通过、耗时、失败原因）
     */
    @Override
    public CheckExecutionResult execute(CheckRule rule, DataCheckMessage message) {
        long startTime = System.nanoTime();

        try {
            // 从缓存获取预编译脚本（避免重复编译）
            CompiledGroovyScript compiledScript = expressionCache.getGroovyScript(rule.getId(), rule.getExpression());
            // 创建 Groovy Binding，注入脚本可访问的变量
            Object result = compiledScript.execute(createBinding(message));
            long duration = System.nanoTime() - startTime;

            if (Boolean.TRUE.equals(result)) {
                return CheckExecutionResult.success(rule.getId(), rule.getMethodPattern(), message.getTraceId(), duration);
            } else {
                return CheckExecutionResult.failure(rule.getId(), rule.getMethodPattern(), message.getTraceId(), duration,
                        "Groovy脚本返回false");
            }
        } catch (Exception e) {
            long duration = System.nanoTime() - startTime;
            log.error("Groovy执行异常: ruleId={}", rule.getId(), e);
            return CheckExecutionResult.failure(rule.getId(), rule.getMethodPattern(), message.getTraceId(), duration,
                    "Groovy执行异常: " + e.getMessage());
        }
    }

    /**
     * 创建 Groovy Binding
     * <p>
     * 注入以下变量供脚本访问:
     * inputParams, returnData, traceId, timestamp, 以及 contextData 中的扩展字段
     */
    private Binding createBinding(DataCheckMessage message) {
        Binding binding = new Binding();
        binding.setVariable("inputParams", message.getInputParams());
        binding.setVariable("returnData", message.getReturnData());
        binding.setVariable("traceId", message.getTraceId());
        binding.setVariable("timestamp", message.getTimestamp());
        // 注入上下文扩展数据
        if (message.getContextData() != null) {
            message.getContextData().forEach(binding::setVariable);
        }
        return binding;
    }
}
