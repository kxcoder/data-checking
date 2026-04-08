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
public class CheckRuleDTO {
    private Long id;
    private String ruleName;
    private String methodPattern;
    private Integer matchType;
    private String ruleType;
    private String expression;
    private Integer priority;
    private Boolean enabled;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
