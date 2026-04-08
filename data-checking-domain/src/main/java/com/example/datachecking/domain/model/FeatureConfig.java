package com.example.datachecking.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String featureKey;
    private String featureName;
    private SupplierType supplierType;
    private String supplierKey;
    private TransformType transformType;
    private String transformScript;
    private String extractPath;
    private Integer cacheTtl;
    private Integer priority;
    private Boolean enabled;

    public enum TransformType {
        NONE,
        SPEL,
        GROOVY
    }

    public boolean isCacheable() {
        return cacheTtl != null && cacheTtl != -1;
    }
}
