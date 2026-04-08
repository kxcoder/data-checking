package com.example.datachecking.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long uid;
    private List<String> featureKeys;
    private Map<String, Object> params;
}
