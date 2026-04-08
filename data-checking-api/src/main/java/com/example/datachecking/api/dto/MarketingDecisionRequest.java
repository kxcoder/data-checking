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
public class MarketingDecisionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sceneCode;

    private Long uid;

    private Map<String, Object> params;

    private Map<String, Object> context;
}