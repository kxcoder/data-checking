package com.example.datachecking.domain.exception;

public enum DomainErrorCode {
    RULE_NOT_FOUND("RULE_NOT_FOUND", "规则不存在"),
    RECORD_NOT_FOUND("RECORD_NOT_FOUND", "检查记录不存在"),
    VERSION_NOT_FOUND("VERSION_NOT_FOUND", "版本不存在"),
    UNSUPPORTED_RULE_TYPE("UNSUPPORTED_RULE_TYPE", "不支持的规则类型"),
    INVALID_OPERATION("INVALID_OPERATION", "无效的操作"),
    ILLEGAL_ARGUMENT("ILLEGAL_ARGUMENT", "非法参数");

    private final String code;
    private final String message;

    DomainErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}