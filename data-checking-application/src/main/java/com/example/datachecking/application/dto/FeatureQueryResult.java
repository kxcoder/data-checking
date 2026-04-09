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
public class FeatureQueryResult {

    private boolean success;
    private Map<String, Object> features;
    private Map<String, String> sources;
    private Map<String, Long> timestamps;
    private String errorMessage;
}
