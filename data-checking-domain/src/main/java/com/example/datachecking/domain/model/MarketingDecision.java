package com.example.datachecking.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketingDecision {
    private String sceneCode;
    private Long uid;
    private Map<String, Object> params;
    private Map<String, Object> context;
    private DecisionResult result;
}