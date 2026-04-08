package com.example.datachecking.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_rule_version")
public class RuleVersionEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ruleId;
    private String ruleName;
    private String methodPattern;
    private Integer matchType;
    private String ruleType;
    private String expression;
    private Integer priority;
    private Boolean enabled;
    private Integer ruleVersion;
    private String status;
    private LocalDateTime publishedAt;
    private String createdBy;
    private LocalDateTime createdAt;
}