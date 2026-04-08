package com.example.datachecking.infrastructure.cache;

import com.example.datachecking.domain.model.CheckRule;
import com.example.datachecking.domain.model.MatchType;
import com.example.datachecking.domain.model.RuleType;
import com.example.datachecking.domain.service.SceneRuleService;
import com.example.datachecking.infrastructure.persistence.entity.CheckRuleEntity;
import com.example.datachecking.infrastructure.persistence.mapper.CheckRuleMapper;
import com.google.common.collect.Maps;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SceneRuleIndexService implements SceneRuleService {

    private final CheckRuleMapper checkRuleMapper;

    private final Map<String, List<CheckRule>> sceneRuleIndex = Maps.newConcurrentMap();

    @PostConstruct
    public void init() {
        rebuildIndex();
        log.info("Scene rule index initialized: scenes={}", sceneRuleIndex.size());
    }

    @Override
    public List<CheckRule> matchRules(String sceneCode) {
        if (sceneCode == null || sceneCode.isBlank()) {
            return List.of();
        }

        List<CheckRule> rules = sceneRuleIndex.get(sceneCode);
        if (rules == null) {
            return List.of();
        }

        return rules.stream()
                .filter(CheckRule::getEnabled)
                .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized void rebuildIndex() {
        log.info("Rebuilding scene rule index...");

        sceneRuleIndex.clear();

        List<CheckRuleEntity> entities = checkRuleMapper.selectList(
                new LambdaQueryWrapper<CheckRuleEntity>()
                        .isNotNull(CheckRuleEntity::getSceneCode)
                        .eq(CheckRuleEntity::getEnabled, true)
        );

        log.info("Loaded {} rules with scene_code", entities.size());

        Map<String, List<CheckRuleEntity>> grouped = entities.stream()
                .collect(Collectors.groupingBy(CheckRuleEntity::getSceneCode));

        for (Map.Entry<String, List<CheckRuleEntity>> entry : grouped.entrySet()) {
            List<CheckRule> rules = entry.getValue().stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
            sceneRuleIndex.put(entry.getKey(), rules);
        }

        log.info("Scene rule index rebuilt: scenes={}", sceneRuleIndex.size());
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
}