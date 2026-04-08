package com.example.datachecking.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketingDecisionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean success;

    private String decisionCode;

    private String message;

    private Map<String, Object> data;

    private Long processTimeMs;
}