package com.example.datachecking.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_check_rule")
public class CheckRuleEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleName;
    private String sceneCode;
    private String methodPattern;
    private Integer matchType;
    private String ruleType;
    private String expression;
    private String outputFields;
    private Integer priority;
    private Boolean enabled;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
