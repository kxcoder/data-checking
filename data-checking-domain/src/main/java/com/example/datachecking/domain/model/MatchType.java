package com.example.datachecking.domain.model;

/**
 * 规则匹配类型枚举
 * <p>
 * 定义规则与方法名的匹配策略，按性能从高到低:
 * <ul>
 *   <li>EXACT — 精确匹配，适用于单一方法的精准核对</li>
 *   <li>WILDCARD — 通配匹配，适用于一组相关方法的批量核对，如 "orderService.*"</li>
 *   <li>REGEX — 正则匹配，适用于复杂模式的匹配，性能最低</li>
 * </ul>
 */
public enum MatchType {
    /** 精确匹配: 方法名完全一致 */
    EXACT(1, "精确匹配"),
    /** 通配匹配: 支持 * 通配符，如 "orderService.*" */
    WILDCARD(2, "通配匹配"),
    /** 正则匹配: 使用 Java 正则表达式 */
    REGEX(3, "正则匹配");

    /** 数据库存储的整数值 */
    private final int code;
    /** 描述文本 */
    private final String description;

    MatchType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    /** 从数据库存储的整数值还原枚举 */
    public static MatchType fromCode(int code) {
        for (MatchType type : values()) {
            if (type.code == code) return type;
        }
        throw new IllegalArgumentException("Unknown MatchType code: " + code);
    }
    
    public static MatchType fromCodeOrNull(Integer code) {
        if (code == null) return null;
        return fromCode(code);
    }
}
