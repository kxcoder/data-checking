package com.example.datachecking.domain.service;

import com.example.datachecking.domain.model.DecisionResult;

import java.util.Map;

public interface MarketingDecisionExecutor {
    DecisionResult execute(String sceneCode, Long uid, Map<String, Object> params, Map<String, Object> context);
}