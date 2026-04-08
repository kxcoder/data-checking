package com.example.datachecking.domain.model;

/**
 * 确认操作类型枚举
 */
public enum ConfirmAction {
    CONFIRM(1, "确认正常"),
    REJECT(2, "标记异常");

    private final int code;
    private final String description;

    ConfirmAction(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static ConfirmAction fromCode(int code) {
        for (ConfirmAction a : values()) {
            if (a.code == code) return a;
        }
        throw new IllegalArgumentException("Unknown ConfirmAction code: " + code);
    }
}