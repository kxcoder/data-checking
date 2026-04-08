package com.example.datachecking.domain.model;

/**
 * 规则引擎类型枚举
 * <p>
 * 选型建议:
 * <ul>
 *   <li>SpEL — 适用于简单条件判断（如 #amount > 0），编译快、内存占用小</li>
 *   <li>Groovy — 适用于复杂业务逻辑（如多层嵌套、日期计算、集合操作），支持完整 Java 语法</li>
 * </ul>
 */
public enum RuleType {
    /** Spring 表达式语言，适用于简单条件判断 */
    SPEL("SPEL", "Spring表达式"),
    /** Groovy 脚本引擎，适用于复杂业务逻辑 */
    GROOVY("GROOVY", "Groovy脚本");

    /** 数据库存储的字符串值 */
    private final String code;
    /** 描述文本 */
    private final String description;

    RuleType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    /** 从数据库存储的字符串值还原枚举 */
    public static RuleType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("RuleType code cannot be null");
        }
        for (RuleType type : values()) {
            if (type.code.equals(code)) return type;
        }
        throw new IllegalArgumentException("Unknown RuleType code: " + code);
    }
}
