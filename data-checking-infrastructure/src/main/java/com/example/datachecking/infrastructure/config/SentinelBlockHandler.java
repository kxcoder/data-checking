package com.example.datachecking.infrastructure.config;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.datachecking.domain.model.DecisionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class SentinelBlockHandler {

    public static DecisionResult handleBlockException(BlockException e) {
        log.warn("Sentinel block triggered: {}", e.getClass().getSimpleName());
        return DecisionResult.builder()
                .success(false)
                .decisionCode("BLOCKED")
                .message("Service temporarily unavailable, please try again later")
                .data(Map.of())
                .processTimeMs(0L)
                .build();
    }

    public static DecisionResult handleFallbackException(Throwable e) {
        log.error("Sentinel fallback triggered", e);
        return DecisionResult.builder()
                .success(false)
                .decisionCode("ERROR")
                .message("Service error: " + e.getMessage())
                .data(Map.of())
                .processTimeMs(0L)
                .build();
    }
}