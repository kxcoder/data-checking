package com.example.datachecking.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleVersion {
    private Long id;
    private Long ruleId;
    private String ruleName;
    private String methodPattern;
    private Integer matchType;
    private String ruleType;
    private String expression;
    private Integer priority;
    private Boolean enabled;
    private Integer ruleVersion;
    private String status;
    private LocalDateTime publishedAt;
    private String createdBy;
    private LocalDateTime createdAt;
}