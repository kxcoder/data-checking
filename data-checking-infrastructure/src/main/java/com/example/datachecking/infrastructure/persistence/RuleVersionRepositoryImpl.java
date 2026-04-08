package com.example.datachecking.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.datachecking.domain.model.RuleVersion;
import com.example.datachecking.domain.repository.RuleVersionRepository;
import com.example.datachecking.infrastructure.persistence.entity.RuleVersionEntity;
import com.example.datachecking.infrastructure.persistence.mapper.RuleVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RuleVersionRepositoryImpl implements RuleVersionRepository {

    private final RuleVersionMapper ruleVersionMapper;

    @Override
    public List<RuleVersion> findByRuleId(Long ruleId) {
        LambdaQueryWrapper<RuleVersionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RuleVersionEntity::getRuleId, ruleId)
               .orderByDesc(RuleVersionEntity::getId);
        return ruleVersionMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<RuleVersion> findByRuleIdAndStatus(Long ruleId, String status) {
        LambdaQueryWrapper<RuleVersionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RuleVersionEntity::getRuleId, ruleId)
               .eq(RuleVersionEntity::getStatus, status);
        return Optional.ofNullable(ruleVersionMapper.selectOne(wrapper)).map(this::toDomain);
    }

    @Override
    public Optional<RuleVersion> findById(Long id) {
        return Optional.ofNullable(ruleVersionMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public RuleVersion save(RuleVersion version) {
        RuleVersionEntity entity = toEntity(version);
        ruleVersionMapper.insert(entity);
        return toDomain(entity);
    }

    @Override
    public void updateStatus(Long id, String status) {
        RuleVersionEntity entity = new RuleVersionEntity();
        entity.setId(id);
        entity.setStatus(status);
        ruleVersionMapper.updateById(entity);
    }

    private RuleVersion toDomain(RuleVersionEntity entity) {
        return RuleVersion.builder()
                .id(entity.getId())
                .ruleId(entity.getRuleId())
                .ruleName(entity.getRuleName())
                .methodPattern(entity.getMethodPattern())
                .matchType(entity.getMatchType())
                .ruleType(entity.getRuleType())
                .expression(entity.getExpression())
                .priority(entity.getPriority())
                .enabled(entity.getEnabled())
                .ruleVersion(entity.getRuleVersion())
                .status(entity.getStatus())
                .publishedAt(entity.getPublishedAt())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private RuleVersionEntity toEntity(RuleVersion version) {
        RuleVersionEntity entity = new RuleVersionEntity();
        entity.setId(version.getId());
        entity.setRuleId(version.getRuleId());
        entity.setRuleName(version.getRuleName());
        entity.setMethodPattern(version.getMethodPattern());
        entity.setMatchType(version.getMatchType());
        entity.setRuleType(version.getRuleType());
        entity.setExpression(version.getExpression());
        entity.setPriority(version.getPriority());
        entity.setEnabled(version.getEnabled());
        entity.setRuleVersion(version.getRuleVersion());
        entity.setStatus(version.getStatus());
        entity.setPublishedAt(version.getPublishedAt());
        entity.setCreatedBy(version.getCreatedBy());
        entity.setCreatedAt(version.getCreatedAt());
        return entity;
    }
}