package com.example.datachecking.domain.event;

public record RuleChangeEvent(Long ruleId, ChangeType changeType) {
    public enum ChangeType {
        CREATE, UPDATE, DELETE, REFRESH_ALL
    }
}
