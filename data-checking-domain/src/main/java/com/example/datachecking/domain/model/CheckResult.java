package com.example.datachecking.domain.model;

public enum CheckResult {
    PASS("PASS", "通过"),
    FAIL("FAIL", "失败");

    private final String code;
    private final String description;

    CheckResult(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static CheckResult fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("CheckResult code cannot be null");
        }
        for (CheckResult r : values()) {
            if (r.code.equals(code)) return r;
        }
        throw new IllegalArgumentException("Unknown CheckResult code: " + code);
    }

    public static CheckResult fromCodeOrNull(String code) {
        if (code == null) return null;
        return fromCode(code);
    }
}
