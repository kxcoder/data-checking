package com.example.datachecking.domain.model;

/**
 * 规则版本状态枚举
 */
public enum VersionStatus {
    DRAFT("draft", "草稿"),
    PUBLISHED("published", "已发布"),
    HISTORY("history", "历史");

    private final String code;
    private final String description;

    VersionStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static VersionStatus fromCode(String code) {
        for (VersionStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        throw new IllegalArgumentException("Unknown VersionStatus code: " + code);
    }
}