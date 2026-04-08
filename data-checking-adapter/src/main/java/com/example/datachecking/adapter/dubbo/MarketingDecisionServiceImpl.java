package com.example.datachecking.adapter.dubbo;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.example.datachecking.api.MarketingDecisionService;
import com.example.datachecking.api.dto.MarketingDecisionRequest;
import com.example.datachecking.api.dto.MarketingDecisionResponse;
import com.example.datachecking.application.dto.MarketingDecisionCommand;
import com.example.datachecking.application.dto.MarketingDecisionResult;
import com.example.datachecking.application.service.MarketingDecisionApplicationService;
import com.example.datachecking.infrastructure.config.SentinelBlockHandler;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(version = "1.0.0", group = "data-checking", timeout = 5000)
@RequiredArgsConstructor
public class MarketingDecisionServiceImpl implements MarketingDecisionService {

    private final MarketingDecisionApplicationService applicationService;

    @Override
    @SentinelResource(
            value = "marketing-decision",
            blockHandler = "handleBlockException",
            fallback = "handleFallbackException"
    )
    public MarketingDecisionResponse decide(MarketingDecisionRequest request) {
        MarketingDecisionCommand command = toCommand(request);
        MarketingDecisionResult result = applicationService.decide(command);
        return toResponse(result);
    }

    private MarketingDecisionCommand toCommand(MarketingDecisionRequest request) {
        return MarketingDecisionCommand.builder()
                .sceneCode(request.getSceneCode())
                .uid(request.getUid())
                .params(request.getParams())
                .context(request.getContext())
                .build();
    }

    private MarketingDecisionResponse toResponse(MarketingDecisionResult result) {
        return MarketingDecisionResponse.builder()
                .success(result.getSuccess())
                .decisionCode(result.getDecisionCode())
                .message(result.getMessage())
                .data(result.getData())
                .processTimeMs(result.getProcessTimeMs())
                .build();
    }
}