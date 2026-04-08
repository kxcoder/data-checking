package com.example.datachecking.application.service;

import com.alibaba.fastjson2.JSONObject;
import com.example.datachecking.api.dto.FeatureQueryRequest;
import com.example.datachecking.api.dto.FeatureQueryResponse;
import com.example.datachecking.domain.model.FeatureConfig;
import com.example.datachecking.domain.model.SupplierType;
import com.example.datachecking.domain.service.DataSupplierService;
import com.example.datachecking.infrastructure.cache.FeatureLocalCache;
import com.example.datachecking.infrastructure.cache.FeatureRedisCache;
import com.example.datachecking.infrastructure.engine.GroovyTransformEngine;
import com.example.datachecking.infrastructure.engine.SpelTransformEngine;
import com.example.datachecking.infrastructure.persistence.entity.FeatureConfigEntity;
import com.example.datachecking.infrastructure.persistence.mapper.FeatureConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureQueryApplicationService {

    private final FeatureConfigMapper featureConfigMapper;
    private final DataSupplierService dataSupplierService;
    private final SpelTransformEngine spelTransformEngine;
    private final GroovyTransformEngine groovyTransformEngine;
    private final FeatureLocalCache localCache;
    private final FeatureRedisCache redisCache;

    public FeatureQueryResponse queryFeatures(FeatureQueryRequest request) {
        try {
            List<String> featureKeys = request.getFeatureKeys();
            Long uid = request.getUid();
            Map<String, Object> params = buildParams(uid, request.getParams());
            
            Map<String, Object> features = new HashMap<>();
            Map<String, String> sources = new HashMap<>();
            Map<String, Long> timestamps = new HashMap<>();
            
            Map<String, FeatureConfig> configMap = loadFeatureConfigs(featureKeys);
            
            Map<String, List<String>> supplierToFeatures = groupBySupplier(configMap);
            
            Map<String, Object> rawDataMap = fetchRawDataSuppliers(supplierToFeatures, params);
            
            for (String featureKey : featureKeys) {
                FeatureConfig config = configMap.get(featureKey);
                if (config == null || !config.getEnabled()) {
                    features.put(featureKey, null);
                    sources.put(featureKey, "NOT_FOUND");
                    continue;
                }
                
                Object rawValue = extractRawValue(config, rawDataMap);
                Object transformedValue = transformValue(config, rawValue);
                
                features.put(featureKey, transformedValue);
                sources.put(featureKey, config.getSupplierType().name());
                timestamps.put(featureKey, System.currentTimeMillis());
                
                if (config.isCacheable()) {
                    cacheFeature(uid, featureKey, transformedValue, config.getCacheTtl());
                }
            }
            
            return FeatureQueryResponse.builder()
                    .features(features)
                    .sources(sources)
                    .timestamps(timestamps)
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("Feature query failed: uid={}", request.getUid(), e);
            return FeatureQueryResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private Map<String, Object> buildParams(Long uid, Map<String, Object> extraParams) {
        Map<String, Object> params = new HashMap<>();
        if (uid != null) {
            params.put("uid", uid);
        }
        if (extraParams != null) {
            params.putAll(extraParams);
        }
        return params;
    }

    private Map<String, FeatureConfig> loadFeatureConfigs(List<String> featureKeys) {
        List<FeatureConfigEntity> entities = featureConfigMapper.selectList(
                new LambdaQueryWrapper<FeatureConfigEntity>()
                        .in(FeatureConfigEntity::getFeatureKey, featureKeys)
                        .eq(FeatureConfigEntity::getEnabled, true)
        );
        
        Map<String, FeatureConfig> configMap = new HashMap<>();
        for (FeatureConfigEntity entity : entities) {
            configMap.put(entity.getFeatureKey(), toFeatureConfig(entity));
        }
        return configMap;
    }

    private Map<String, List<String>> groupBySupplier(Map<String, FeatureConfig> configMap) {
        Map<String, List<String>> supplierToFeatures = new HashMap<>();
        for (Map.Entry<String, FeatureConfig> entry : configMap.entrySet()) {
            String featureKey = entry.getKey();
            FeatureConfig config = entry.getValue();
            supplierToFeatures
                    .computeIfAbsent(config.getSupplierKey(), k -> new ArrayList<>())
                    .add(featureKey);
        }
        return supplierToFeatures;
    }

    private Map<String, Object> fetchRawDataSuppliers(Map<String, List<String>> supplierToFeatures, Map<String, Object> params) {
        List<CompletableFuture<Map<String, Object>>> futures = supplierToFeatures.keySet()
                .stream()
                .map(supplierKey -> CompletableFuture.supplyAsync(() -> {
                    Object rawData = dataSupplierService.supply(supplierKey, params);
                    Map<String, Object> result = new HashMap<>();
                    for (String featureKey : supplierToFeatures.get(supplierKey)) {
                        result.put(featureKey, rawData);
                    }
                    return result;
                }))
                .collect(Collectors.toList());
        
        Map<String, Object> rawDataMap = new HashMap<>();
        for (CompletableFuture<Map<String, Object>> f : futures) {
            try {
                rawDataMap.putAll(f.get());
            } catch (Exception e) {
                log.error("Failed to fetch raw data", e);
            }
        }
        return rawDataMap;
    }

    private Object extractRawValue(FeatureConfig config, Map<String, Object> rawDataMap) {
        String extractPath = config.getExtractPath();
        if (extractPath == null || extractPath.isEmpty()) {
            return rawDataMap.get(config.getFeatureKey());
        }
        
        Object rawData = rawDataMap.get(config.getFeatureKey());
        if (rawData == null) {
            return null;
        }
        
        if (rawData instanceof JSONObject) {
            return extractByPath((JSONObject) rawData, extractPath);
        }
        return rawData;
    }

    private Object extractByPath(JSONObject json, String path) {
        String[] parts = path.split("\\.");
        Object current = json;
        for (String part : parts) {
            if (current instanceof JSONObject) {
                current = ((JSONObject) current).get(part);
            } else if (current instanceof com.alibaba.fastjson2.JSONArray) {
                try {
                    int idx = Integer.parseInt(part);
                    current = ((com.alibaba.fastjson2.JSONArray) current).get(idx);
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return current;
    }

    private Object transformValue(FeatureConfig config, Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        
        FeatureConfig.TransformType transformType = config.getTransformType();
        if (transformType == null || transformType == FeatureConfig.TransformType.NONE) {
            return rawValue;
        }
        
        String script = config.getTransformScript();
        return switch (transformType) {
            case SPEL -> spelTransformEngine.transform(script, rawValue);
            case GROOVY -> groovyTransformEngine.transform(script, rawValue);
            default -> rawValue;
        };
    }

    private void cacheFeature(Long uid, String featureKey, Object value, int ttlSeconds) {
        String cacheKey = uid + ":" + featureKey;
        localCache.put(cacheKey, value, ttlSeconds);
        redisCache.put(cacheKey, value, ttlSeconds);
    }

    private FeatureConfig toFeatureConfig(FeatureConfigEntity entity) {
        return FeatureConfig.builder()
                .id(entity.getId())
                .featureKey(entity.getFeatureKey())
                .featureName(entity.getFeatureName())
                .supplierType(SupplierType.valueOf(entity.getSupplierType()))
                .supplierKey(entity.getSupplierKey())
                .transformType(FeatureConfig.TransformType.valueOf(entity.getTransformType()))
                .transformScript(entity.getTransformScript())
                .extractPath(entity.getExtractPath())
                .cacheTtl(entity.getCacheTtl())
                .priority(entity.getPriority())
                .enabled(entity.getEnabled())
                .build();
    }
}
