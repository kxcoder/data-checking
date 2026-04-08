package com.example.datachecking.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.datachecking.domain.model.CheckRule;
import com.example.datachecking.domain.model.MatchType;
import com.example.datachecking.domain.model.RuleType;
import com.example.datachecking.domain.repository.CheckRuleRepository;
import com.example.datachecking.infrastructure.persistence.entity.CheckRuleEntity;
import com.example.datachecking.infrastructure.persistence.mapper.CheckRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CheckRuleRepositoryImpl implements CheckRuleRepository {

    private final CheckRuleMapper checkRuleMapper;

    @Override
    public Optional<CheckRule> findById(Long id) {
        CheckRuleEntity entity = checkRuleMapper.selectById(id);
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public List<CheckRule> findAllEnabled() {
        LambdaQueryWrapper<CheckRuleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckRuleEntity::getEnabled, true)
               .orderByDesc(CheckRuleEntity::getPriority);
        return checkRuleMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public CheckRule save(CheckRule rule) {
        CheckRuleEntity entity = toEntity(rule);
        if (entity.getId() == null) {
            checkRuleMapper.insert(entity);
        } else {
            checkRuleMapper.updateById(entity);
        }
        return toDomain(entity);
    }

    @Override
    public void deleteById(Long id) {
        checkRuleMapper.deleteById(id);
    }

    @Override
    public List<CheckRule> findByMethodPattern(String methodPattern) {
        LambdaQueryWrapper<CheckRuleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckRuleEntity::getMethodPattern, methodPattern);
        return checkRuleMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .toList();
    }

    private CheckRule toDomain(CheckRuleEntity entity) {
        return CheckRule.builder()
                .id(entity.getId())
                .ruleName(entity.getRuleName())
                .methodPattern(entity.getMethodPattern())
                .matchType(MatchType.fromCode(entity.getMatchType()))
                .ruleType(RuleType.fromCode(entity.getRuleType()))
                .expression(entity.getExpression())
                .priority(entity.getPriority())
                .enabled(entity.getEnabled())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private CheckRuleEntity toEntity(CheckRule rule) {
        CheckRuleEntity entity = new CheckRuleEntity();
        entity.setId(rule.getId());
        entity.setRuleName(rule.getRuleName());
        entity.setMethodPattern(rule.getMethodPattern());
        entity.setMatchType(rule.getMatchType().getCode());
        entity.setRuleType(rule.getRuleType().getCode());
        entity.setExpression(rule.getExpression());
        entity.setPriority(rule.getPriority());
        entity.setEnabled(rule.getEnabled());
        entity.setVersion(rule.getVersion());
        entity.setCreatedAt(rule.getCreatedAt());
        entity.setUpdatedAt(rule.getUpdatedAt());
        return entity;
    }
}
