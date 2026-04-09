package com.example.datachecking.domain.repository;

import com.example.datachecking.domain.model.FeatureConfig;

import java.util.List;

public interface FeatureConfigRepository {
    
    List<FeatureConfig> findByFeatureKeys(List<String> featureKeys);
    
    FeatureConfig findByFeatureKey(String featureKey);
}
