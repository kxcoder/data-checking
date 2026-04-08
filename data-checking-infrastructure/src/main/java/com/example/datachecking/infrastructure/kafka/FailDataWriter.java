package com.example.datachecking.infrastructure.kafka;

import com.example.datachecking.domain.model.CheckExecutionResult;
import com.example.datachecking.domain.model.CheckRecord;
import com.example.datachecking.domain.model.CheckResult;
import com.example.datachecking.domain.model.ConfirmStatus;
import com.example.datachecking.domain.repository.CheckRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FailDataWriter {

    private final CheckRecordRepository checkRecordRepository;

    @Async("failWriteExecutor")
    public void writeBatchAsync(List<CheckExecutionResult> failedResults) {
        if (failedResults == null || failedResults.isEmpty()) {
            return;
        }
        
        for (CheckExecutionResult result : failedResults) {
            try {
                CheckRecord record = CheckRecord.builder()
                        .traceId(result.getTraceId())
                        .ruleId(result.getRuleId())
                        .methodName(result.getMethodName())
                        .checkResult(result.isPassed() ? CheckResult.PASS : CheckResult.FAIL)
                        .failReason(result.getFailReason())
                        .confirmStatus(ConfirmStatus.PENDING)
                        .build();
                checkRecordRepository.save(record);
            } catch (Exception e) {
                log.error("写入失败数据异常: ruleId={}, methodName={}", 
                        result.getRuleId(), result.getMethodName(), e);
            }
        }
        log.info("异步写入失败数据完成, count={}", failedResults.size());
    }
}
