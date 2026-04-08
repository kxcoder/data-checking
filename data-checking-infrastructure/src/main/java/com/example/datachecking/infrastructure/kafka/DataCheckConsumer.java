package com.example.datachecking.infrastructure.kafka;

import com.alibaba.fastjson2.JSON;
import com.example.datachecking.domain.model.CheckExecutionResult;
import com.example.datachecking.domain.model.CheckRule;
import com.example.datachecking.domain.model.DataCheckMessage;
import com.example.datachecking.domain.service.CheckEngine;
import com.example.datachecking.domain.service.RuleMatchService;
import com.example.datachecking.infrastructure.cache.CompiledExpressionCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataCheckConsumer {

    private final RuleMatchService ruleMatchService;
    private final CheckEngine checkEngine;
    private final CheckMetricsCollector metricsCollector;
    private final FailDataWriter failDataWriter;
    private final CompiledExpressionCache expressionCache;

    @KafkaListener(topics = "${data-check.kafka.topic:data-check-msg}", groupId = "${data-check.kafka.group-id:data-check-group}")
    public void consume(List<String> messages, Acknowledgment ack) {
        log.info("收到Kafka批量消息, size={}", messages.size());

        List<CheckExecutionResult> allResults = new ArrayList<>();
        int failedCount = 0;
        
        for (String json : messages) {
            try {
                List<CheckExecutionResult> results = processMessage(json);
                allResults.addAll(results);
                
                for (CheckExecutionResult r : results) {
                    if (!r.isPassed()) {
                        failedCount++;
                    }
                }
            } catch (Exception e) {
                log.error("处理消息异常: {}", json, e);
                failedCount++;
            }
        }

        if (failedCount > 0) {
            ack.nack(failedCount, Duration.ofMillis(0));
            log.warn("消息处理失败, 不 ACK, failedCount={}", failedCount);
            return;
        }

        metricsCollector.recordBatch(allResults);
        
        List<CheckExecutionResult> failed = allResults.stream()
                .filter(r -> !r.isPassed())
                .toList();
        if (!failed.isEmpty()) {
            failDataWriter.writeBatchAsync(failed);
        }

        ack.acknowledge();
        log.info("消息处理完成, 总数={}, 失败={}", allResults.size(), failed.size());
    }

    private List<CheckExecutionResult> processMessage(String json) {
        DataCheckMessage message = JSON.parseObject(json, DataCheckMessage.class);
        
        if (message.getMethodName() == null || message.getMethodName().isBlank()) {
            return List.of(CheckExecutionResult.failure(null, "unknown", null, 0, "methodName为空"));
        }

        List<CheckRule> rules = ruleMatchService.matchRules(message.getMethodName());

        if (rules.isEmpty()) {
            metricsCollector.recordSkipped(message.getMethodName());
            return List.of();
        }

        List<CheckExecutionResult> results = new ArrayList<>();
        for (CheckRule rule : rules) {
            CheckExecutionResult result = checkEngine.execute(rule, message);
            results.add(result);
            
            var stats = expressionCache.getStats();
            if (stats.spelStats().hitCount() > 0 || stats.groovyStats().hitCount() > 0) {
                metricsCollector.recordCacheHit();
            } else {
                metricsCollector.recordCacheMiss();
            }
        }
        return results;
    }
}