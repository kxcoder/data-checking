package com.example.datachecking.infrastructure.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SentinelRuleConfig {

    @EventListener(ApplicationReadyEvent.class)
    public void initSentinelRules() {
        initFlowRules();
        initDegradeRules();
        log.info("Sentinel rules initialized");
    }

    private void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        FlowRule rule = new FlowRule("marketing-decision");
        rule.setResource("marketing-decision");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(100);
        rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        rule.setMaxQueueingTimeMs(500);
        rules.add(rule);

        FlowRuleManager.loadRules(rules);
        log.info("Flow rules loaded: qps=100");
    }

    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        DegradeRule rule = new DegradeRule("marketing-decision");
        rule.setResource("marketing-decision");
        rule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        rule.setCount(0.5);
        rule.setTimeWindow(10);
        rule.setMinRequestAmount(5);
        rule.setStatIntervalMs(10000);

        rules.add(rule);

        DegradeRuleManager.loadRules(rules);
        log.info("Degrade rules loaded: errorRatio=50%, timeWindow=10s");
    }
}