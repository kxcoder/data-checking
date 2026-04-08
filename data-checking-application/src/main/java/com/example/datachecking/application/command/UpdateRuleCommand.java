package com.example.datachecking.application.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRuleCommand {
    @NotNull Long id;
    String ruleName;
    String methodPattern;
    Integer matchType;
    String ruleType;
    String expression;
    Integer priority;
    Boolean enabled;
}
