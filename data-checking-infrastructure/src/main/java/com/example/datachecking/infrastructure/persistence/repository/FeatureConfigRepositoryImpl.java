package com.example.datachecking.infrastructure.persistence.repository;

import com.example.datachecking.domain.model.FeatureConfig;
import com.example.datachecking.domain.model.SupplierType;
import com.example.datachecking.domain.repository.FeatureConfigRepository;
import com.example.datachecking.infrastructure.persistence.entity.FeatureConfigEntity;
import com.example.datachecking.infrastructure.persistence.mapper.FeatureConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class FeatureConfigRepositoryImpl implements FeatureConfigRepository {

    private final FeatureConfigMapper featureConfigMapper;

    @Override
    public List<FeatureConfig> findByFeatureKeys(List<String> featureKeys) {
        List<FeatureConfigEntity> entities = featureConfigMapper.selectList(
                new LambdaQueryWrapper<FeatureConfigEntity>()
                        .in(FeatureConfigEntity::getFeatureKey, featureKeys)
                        .eq(FeatureConfigEntity::getEnabled, true)
        );
        return entities.stream().map(this::toFeatureConfig).collect(Collectors.toList());
    }

    @Override
    public FeatureConfig findByFeatureKey(String featureKey) {
        FeatureConfigEntity entity = featureConfigMapper.selectOne(
                new LambdaQueryWrapper<FeatureConfigEntity>()
                        .eq(FeatureConfigEntity::getFeatureKey, featureKey)
                        .eq(FeatureConfigEntity::getEnabled, true)
        );
        return entity != null ? toFeatureConfig(entity) : null;
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
