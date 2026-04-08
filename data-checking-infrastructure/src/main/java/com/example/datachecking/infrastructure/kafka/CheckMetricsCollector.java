package com.example.datachecking.infrastructure.kafka;

import com.example.datachecking.domain.model.CheckExecutionResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class CheckMetricsCollector {

    private final Counter successCounter;
    private final Counter failCounter;
    private final Timer checkDurationTimer;
    private final Counter expressionCacheHit;
    private final Counter expressionCacheMiss;
    private final Counter skippedCounter;

    public CheckMetricsCollector(MeterRegistry registry) {
        this.successCounter = Counter.builder("data_check_total")
                .tag("result", "success")
                .register(registry);
        this.failCounter = Counter.builder("data_check_total")
                .tag("result", "fail")
                .register(registry);
        this.checkDurationTimer = Timer.builder("data_check_duration_seconds")
                .register(registry);
        this.expressionCacheHit = Counter.builder("expression_cache_total")
                .tag("result", "hit")
                .register(registry);
        this.expressionCacheMiss = Counter.builder("expression_cache_total")
                .tag("result", "miss")
                .register(registry);
        this.skippedCounter = Counter.builder("data_check_skipped_total")
                .register(registry);
    }

    public void recordBatch(List<CheckExecutionResult> results) {
        for (CheckExecutionResult result : results) {
            if (result == null) continue;
            if (result.isPassed()) {
                successCounter.increment();
            } else {
                failCounter.increment();
            }
            checkDurationTimer.record(result.getDurationNanos(), TimeUnit.NANOSECONDS);
        }
    }

    public void recordSkipped(String methodName) {
        skippedCounter.increment();
    }

    public void recordCacheHit() {
        expressionCacheHit.increment();
    }

    public void recordCacheMiss() {
        expressionCacheMiss.increment();
    }
}
