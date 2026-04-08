package com.example.datachecking.domain.model;

public enum ConfirmStatus {
    PENDING(0, "待确认"),
    CONFIRMED_NORMAL(1, "已确认正常"),
    CONFIRMED_ABNORMAL(2, "已确认异常");

    private final int code;
    private final String description;

    ConfirmStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static ConfirmStatus fromCode(int code) {
        for (ConfirmStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown ConfirmStatus code: " + code);
    }

    public static ConfirmStatus fromCodeOrNull(Integer code) {
        if (code == null) return null;
        return fromCode(code);
    }
}
