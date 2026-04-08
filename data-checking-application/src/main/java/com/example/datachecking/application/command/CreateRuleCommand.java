package com.example.datachecking.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRuleCommand {
    @NotBlank String ruleName;
    @NotBlank String methodPattern;
    @NotNull Integer matchType;
    @NotBlank String ruleType;
    @NotBlank String expression;
    Integer priority;
    Boolean enabled;
}
