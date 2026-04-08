package com.example.datachecking.infrastructure.engine;

import com.example.datachecking.domain.model.CheckExecutionResult;
import com.example.datachecking.domain.model.CheckRule;
import com.example.datachecking.domain.model.DataCheckMessage;
import com.example.datachecking.domain.model.RuleType;
import com.example.datachecking.domain.service.CheckEngine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 引擎分发器 — 策略模式的实现
 * <p>
 * 职责: 根据规则的 RuleType 将执行请求分发到对应的引擎实现。
 * <p>
 * 设计: Spring 启动时自动收集所有 CheckEngine 实现，通过 getRuleType() 注册到 EnumMap。
 * 新增引擎只需实现 CheckEngine 接口并标注 @Component，无需修改分发器代码。
 */
@Slf4j
@Component
@Primary
public class CheckEngineDispatcher implements CheckEngine {

    /** 规则类型 → 引擎实现的映射 (EnumMap 类型安全 + 高性能) */
    private final Map<RuleType, CheckEngine> engineMap = new EnumMap<>(RuleType.class);

    @Autowired
    private List<CheckEngine> engines;

    @Override
    public RuleType getRuleType() {
        throw new UnsupportedOperationException("CheckEngineDispatcher是分发器，不直接对应规则类型");
    }

    @PostConstruct
    public void registerEngines() {
        for (CheckEngine engine : engines) {
            // 跳过自己，避免循环注册
            if (engine instanceof CheckEngineDispatcher) {
                continue;
            }
            RuleType ruleType = engine.getRuleType();
            engineMap.put(ruleType, engine);
            log.info("注册核对引擎: ruleType={}, engine={}", ruleType, engine.getClass().getSimpleName());
        }
    }

    /**
     * 根据规则类型分发到对应引擎执行
     *
     * @param rule    核对规则
     * @param message Kafka 消息数据
     * @return 执行结果
     */
    @Override
    public CheckExecutionResult execute(CheckRule rule, DataCheckMessage message) {
        CheckEngine engine = engineMap.get(rule.getRuleType());
        if (engine == null) {
            throw new IllegalArgumentException("不支持的规则类型: " + rule.getRuleType());
        }
        return engine.execute(rule, message);
    }
}
