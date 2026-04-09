package com.example.datachecking.adapter.dubbo;

import com.example.datachecking.api.FeatureQueryService;
import com.example.datachecking.api.dto.FeatureQueryRequest;
import com.example.datachecking.api.dto.FeatureQueryResponse;
import com.example.datachecking.application.dto.FeatureQueryCommand;
import com.example.datachecking.application.dto.FeatureQueryResult;
import com.example.datachecking.application.service.FeatureQueryApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

@Slf4j
@DubboService(version = "1.0.0", group = "data-checking", timeout = 5000)
@RequiredArgsConstructor
public class FeatureQueryServiceImpl implements FeatureQueryService {

    private final FeatureQueryApplicationService featureQueryApplicationService;

    @Override
    public FeatureQueryResponse queryFeatures(FeatureQueryRequest request) {
        log.info("Feature query request: uid={}, keys={}", request.getUid(), request.getFeatureKeys());
        
        FeatureQueryCommand command = FeatureQueryCommand.builder()
                .uid(request.getUid())
                .featureKeys(request.getFeatureKeys())
                .params(request.getParams())
                .build();
        
        FeatureQueryResult result = featureQueryApplicationService.queryFeatures(command);
        
        return FeatureQueryResponse.builder()
                .success(result.isSuccess())
                .features(result.getFeatures())
                .sources(result.getSources())
                .timestamps(result.getTimestamps())
                .errorMessage(result.getErrorMessage())
                .build();
    }
}
