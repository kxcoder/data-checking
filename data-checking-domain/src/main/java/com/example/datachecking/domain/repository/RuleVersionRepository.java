package com.example.datachecking.domain.repository;

import com.example.datachecking.domain.model.RuleVersion;
import java.util.List;
import java.util.Optional;

public interface RuleVersionRepository {
    List<RuleVersion> findByRuleId(Long ruleId);
    Optional<RuleVersion> findByRuleIdAndStatus(Long ruleId, String status);
    Optional<RuleVersion> findById(Long id);
    RuleVersion save(RuleVersion version);
    void updateStatus(Long id, String status);
}