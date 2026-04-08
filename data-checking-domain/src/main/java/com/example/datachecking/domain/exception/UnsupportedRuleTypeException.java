package com.example.datachecking.domain.exception;

public class UnsupportedRuleTypeException extends DataCheckingDomainException {
    public UnsupportedRuleTypeException(String ruleType) {
        super(DomainErrorCode.UNSUPPORTED_RULE_TYPE, "不支持的规则类型: " + ruleType);
    }
}