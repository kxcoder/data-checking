package com.example.datachecking.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckRuleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

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
