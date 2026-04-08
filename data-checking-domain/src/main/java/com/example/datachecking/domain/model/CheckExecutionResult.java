package com.example.datachecking.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 核对执行结果值对象
 * <p>
 * 记录单次规则执行的结果，包含是否通过、失败原因、执行耗时等信息。
 * 由 CheckEngine 执行后返回，用于指标上报和失败数据入库。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckExecutionResult {
    /** 是否通过核验 */
    private boolean passed;
    /** 失败原因，仅在 passed=false 时有值 */
    private String failReason;
    /** 执行的规则ID */
    private Long ruleId;
    /** 被核对的方法名 */
    private String methodName;
    /** 执行耗时（纳秒），用于性能监控 */
    private long durationNanos;
    /** 链路追踪ID */
    private String traceId;

    /**
     * 工厂方法: 创建成功结果
     */
    public static CheckExecutionResult success(Long ruleId, String methodName, String traceId, long durationNanos) {
        return CheckExecutionResult.builder()
                .passed(true)
                .ruleId(ruleId)
                .methodName(methodName)
                .traceId(traceId)
                .durationNanos(durationNanos)
                .build();
    }

    /**
     * 工厂方法: 创建失败结果
     */
    public static CheckExecutionResult failure(Long ruleId, String methodName, String traceId, long durationNanos, String failReason) {
        return CheckExecutionResult.builder()
                .passed(false)
                .ruleId(ruleId)
                .methodName(methodName)
                .traceId(traceId)
                .durationNanos(durationNanos)
                .failReason(failReason)
                .build();
    }
}
