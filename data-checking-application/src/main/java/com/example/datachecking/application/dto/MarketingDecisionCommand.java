package com.example.datachecking.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketingDecisionCommand {

    private String sceneCode;

    private Long uid;

    private Map<String, Object> params;

    private Map<String, Object> context;
}