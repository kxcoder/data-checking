package com.example.datachecking.api;

import com.example.datachecking.api.dto.FeatureQueryRequest;
import com.example.datachecking.api.dto.FeatureQueryResponse;

public interface FeatureQueryService {
    
    FeatureQueryResponse queryFeatures(FeatureQueryRequest request);
}
