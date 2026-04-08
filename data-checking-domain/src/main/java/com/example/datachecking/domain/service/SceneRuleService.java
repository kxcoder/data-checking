package com.example.datachecking.domain.service;

import com.example.datachecking.domain.model.CheckRule;

import java.util.List;

public interface SceneRuleService {
    List<CheckRule> matchRules(String sceneCode);
    void rebuildIndex();
}