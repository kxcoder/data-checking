package com.example.datachecking.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckRecordDTO {
    private Long id;
    private String traceId;
    private Long ruleId;
    private String methodName;
    private String checkResult;
    private String expression;
    private String inputParams;
    private String returnData;
    private String failReason;
    private Integer confirmStatus;
    private String confirmUser;
    private String confirmRemark;
    private LocalDateTime confirmAt;
    private LocalDateTime createdAt;
}
