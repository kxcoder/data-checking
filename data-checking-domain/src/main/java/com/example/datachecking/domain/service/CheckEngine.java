package com.example.datachecking.domain.service;

import com.example.datachecking.domain.model.CheckExecutionResult;
import com.example.datachecking.domain.model.CheckRule;
import com.example.datachecking.domain.model.DataCheckMessage;
import com.example.datachecking.domain.model.RuleType;

/**
 * 核对引擎领域服务接口
 * <p>
 * 职责: 执行单次数据核对，根据规则表达式核验消息数据是否符合预期。
 * <p>
 * DDD 角色: 策略模式的接口定义。两种实现:
 * <ul>
 *   <li>SpelCheckEngine — 适用于简单条件判断，轻量高效</li>
 *   <li>GroovyCheckEngine — 适用于复杂业务逻辑，灵活强大</li>
 * </ul>
 * 通过 CheckEngineDispatcher 按 RuleType 分发到对应实现。
 */
public interface CheckEngine {

    /**
     * 返回该引擎支持的规则类型
     * 分发器根据此方法自动注册引擎映射
     */
    RuleType getRuleType();

    /**
     * 执行单次核对
     *
     * @param rule    核对规则（包含预编译表达式）
     * @param message Kafka 消息数据（包含方法名、入参、返回值）
     * @return 核对执行结果（包含是否通过、失败原因、耗时等）
     */
    CheckExecutionResult execute(CheckRule rule, DataCheckMessage message);
}
