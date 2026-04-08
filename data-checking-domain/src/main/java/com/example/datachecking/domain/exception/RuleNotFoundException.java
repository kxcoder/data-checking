package com.example.datachecking.domain.exception;

public class RuleNotFoundException extends DataCheckingDomainException {
    public RuleNotFoundException(Long id) {
        super(DomainErrorCode.RULE_NOT_FOUND, "规则不存在: " + id);
    }

    public RuleNotFoundException(String message) {
        super(DomainErrorCode.RULE_NOT_FOUND, message);
    }
}