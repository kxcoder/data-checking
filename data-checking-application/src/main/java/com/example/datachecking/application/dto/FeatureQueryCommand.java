package com.example.datachecking.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureQueryCommand {

    private Long uid;
    private List<String> featureKeys;
    private Map<String, Object> params;
}
