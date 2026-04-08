package com.example.datachecking.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 核对规则聚合根
 * <p>
 * 业务含义: 定义一条数据核对规则，包括匹配方法名、表达式引擎、核验表达式等。
 * 系统通过 Kafka 接收业务系统的消息后，根据消息中的方法名匹配规则，
 * 执行对应的 SpEL 表达式或 Groovy 脚本进行数据核验。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckRule {
    private Long id;
    private String ruleName;
    private String methodPattern;
    private MatchType matchType;
    private RuleType ruleType;
    private String expression;
    private Integer priority;
    private Boolean enabled;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isApplicableTo(String methodName) {
        if (methodName == null) {
            return false;
        }
        if (enabled == null || !enabled) {
            return false;
        }
        if (matchType == null) {
            return false;
        }
        return switch (matchType) {
            case EXACT -> methodName.equals(methodPattern);
            case WILDCARD -> matchWildcard(methodPattern, methodName);
            case REGEX -> methodPattern != null && methodName.matches(methodPattern);
        };
    }

    private boolean matchWildcard(String pattern, String methodName) {
        if (pattern == null || methodName == null) {
            return false;
        }
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*");
        return methodName.matches(regex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CheckRule checkRule = (CheckRule) o;
        return Objects.equals(id, checkRule.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}