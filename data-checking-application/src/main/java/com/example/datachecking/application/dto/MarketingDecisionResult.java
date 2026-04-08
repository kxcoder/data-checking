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
public class MarketingDecisionResult {

    private Boolean success;

    private String decisionCode;

    private String message;

    private Map<String, Object> data;

    private Long processTimeMs;
}