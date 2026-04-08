package com.example.datachecking.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureQueryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Builder.Default
    private Map<String, Object> features = new HashMap<>();
    
    @Builder.Default
    private Map<String, String> sources = new HashMap<>();
    
    @Builder.Default
    private Map<String, Long> timestamps = new HashMap<>();
    
    private boolean success;
    private String errorMessage;
}
