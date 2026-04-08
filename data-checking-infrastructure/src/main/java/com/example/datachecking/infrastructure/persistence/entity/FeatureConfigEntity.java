package com.example.datachecking.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_feature_config")
public class FeatureConfigEntity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    private String featureKey;
    private String featureName;
    private String supplierType;
    private String supplierKey;
    private String transformType;
    private String transformScript;
    private String extractPath;
    private Integer cacheTtl;
    private Integer priority;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
